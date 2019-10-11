/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Metadata importer to allow the batch import of metadata from a file
 *
 * @author Stuart Lewis
 */
public class MetadataImport {
    /**
     * The Context
     */
    Context c;

    /**
     * The DSpaceCSV object we're processing
     */
    DSpaceCSV csv;

    /**
     * The lines to import
     */
    List<DSpaceCSVLine> toImport;

    /**
     * The authority controlled fields
     */
    protected static Set<String> authorityControlled;

    static {
        setAuthorizedMetadataFields();
    }

    /**
     * The prefix of the authority controlled field
     */
    protected static final String AC_PREFIX = "authority.controlled.";

    /**
     * Map of field:value to csv row number, used to resolve indirect entity references.
     *
     * @see #populateRefAndRowMap(DSpaceCSVLine, int, UUID)
     */
    protected HashMap<String, Set<Integer>> csvRefMap = new HashMap<>();

    /**
     * Map of csv row number to UUID, used to resolve indirect entity references.
     *
     * @see #populateRefAndRowMap(DSpaceCSVLine, int, UUID)
     */
    protected HashMap<Integer, UUID> csvRowMap = new HashMap<>();

    /**
     * Logger
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataImport.class);

    protected final AuthorityValueService authorityValueService;

    protected final ItemService itemService;
    protected final InstallItemService installItemService;
    protected final CollectionService collectionService;
    protected final HandleService handleService;
    protected final WorkspaceItemService workspaceItemService;
    protected final RelationshipTypeService relationshipTypeService;
    protected final RelationshipService relationshipService;
    protected final EntityTypeService entityTypeService;
    protected final EntityService entityService;

    /**
     * Create an instance of the metadata importer. Requires a context and an array of CSV lines
     * to examine.
     *
     * @param c        The context
     * @param toImport An array of CSV lines to examine
     */
    public MetadataImport(Context c, DSpaceCSV toImport) {
        // Store the import settings
        this.c = c;
        csv = toImport;
        this.toImport = toImport.getCSVLines();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        authorityValueService = AuthorityServiceFactory.getInstance().getAuthorityValueService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        entityService = ContentServiceFactory.getInstance().getEntityService();
    }

    /**
     * Run an import. The import can either be read-only to detect changes, or
     * can write changes as it goes.
     *
     * @param change         Whether or not to write the changes to the database
     * @param useWorkflow    Whether the workflows should be used when creating new items
     * @param workflowNotify If the workflows should be used, whether to send notifications or not
     * @param useTemplate    Use collection template if create new item
     * @return An array of BulkEditChange elements representing the items that have changed
     * @throws MetadataImportException if something goes wrong
     */
    public List<BulkEditChange> runImport(boolean change,
                                          boolean useWorkflow,
                                          boolean workflowNotify,
                                          boolean useTemplate) throws MetadataImportException {
        // Store the changes
        ArrayList<BulkEditChange> changes = new ArrayList<BulkEditChange>();

        // Make the changes
        try {
            Context.Mode originalMode = c.getCurrentMode();
            c.setMode(Context.Mode.BATCH_EDIT);

            // Process each change
            int rowCount = 1;
            for (DSpaceCSVLine line : toImport) {
                //Resolve references to other items
                populateRefAndRowMap(line, rowCount, null);
                line = resolveEntityRefs(line);
                // Get the DSpace item to compare with
                UUID id = line.getID();

                // Is there an action column?
                if (csv.hasActions() && (!"".equals(line.getAction())) && (id == null)) {
                    throw new MetadataImportException("'action' not allowed for new items!");
                }

                WorkspaceItem wsItem = null;
                WorkflowItem wfItem = null;
                Item item = null;

                // Is this a new item?
                if (id != null) {
                    // Get the item
                    item = itemService.find(c, id);
                    if (item == null) {
                        throw new MetadataImportException("Unknown item ID " + id);
                    }

                    // Record changes
                    BulkEditChange whatHasChanged = new BulkEditChange(item);

                    // Has it moved collection?
                    List<String> collections = line.get("collection");
                    if (collections != null) {
                        // Sanity check we're not orphaning it
                        if (collections.size() == 0) {
                            throw new MetadataImportException("Missing collection from item " + item.getHandle());
                        }
                        List<Collection> actualCollections = item.getCollections();
                        compare(item, collections, actualCollections, whatHasChanged, change);
                    }

                    // Iterate through each metadata element in the csv line
                    for (String md : line.keys()) {
                        // Get the values we already have
                        if (!"id".equals(md)) {
                            // Get the values from the CSV
                            String[] fromCSV = line.get(md).toArray(new String[line.get(md).size()]);
                            // Remove authority unless the md is not authority controlled
                            if (!isAuthorityControlledField(md)) {
                                for (int i = 0; i < fromCSV.length; i++) {
                                    int pos = fromCSV[i].indexOf(csv.getAuthoritySeparator());
                                    if (pos > -1) {
                                        fromCSV[i] = fromCSV[i].substring(0, pos);
                                    }
                                }
                            }

                            // Compare
                            compare(item, fromCSV, change, md, whatHasChanged, line);
                        }
                    }

                    if (csv.hasActions()) {
                        // Perform the action
                        String action = line.getAction();
                        if ("".equals(action)) {
                            // Do nothing
                        } else if ("expunge".equals(action)) {
                            // Does the configuration allow deletes?
                            if (!ConfigurationManager.getBooleanProperty("bulkedit", "allowexpunge", false)) {
                                throw new MetadataImportException("'expunge' action denied by configuration");
                            }

                            // Remove the item

                            if (change) {
                                itemService.delete(c, item);
                            }

                            whatHasChanged.setDeleted();
                        } else if ("withdraw".equals(action)) {
                            // Withdraw the item
                            if (!item.isWithdrawn()) {
                                if (change) {
                                    itemService.withdraw(c, item);
                                }
                                whatHasChanged.setWithdrawn();
                            }
                        } else if ("reinstate".equals(action)) {
                            // Reinstate the item
                            if (item.isWithdrawn()) {
                                if (change) {
                                    itemService.reinstate(c, item);
                                }
                                whatHasChanged.setReinstated();
                            }
                        } else {
                            // Unknown action!
                            throw new MetadataImportException("Unknown action: " + action);
                        }
                    }

                    // Only record if changes have been made
                    if (whatHasChanged.hasChanges()) {
                        changes.add(whatHasChanged);
                    }
                } else {
                    // This is marked as a new item, so no need to compare

                    // First check a user is set, otherwise this can't happen
                    if (c.getCurrentUser() == null) {
                        throw new MetadataImportException(
                            "When adding new items, a user must be specified with the -e option");
                    }

                    // Iterate through each metadata element in the csv line
                    BulkEditChange whatHasChanged = new BulkEditChange();
                    for (String md : line.keys()) {
                        // Get the values we already have
                        if (!"id".equals(md) && !"rowName".equals(md)) {
                            // Get the values from the CSV
                            String[] fromCSV = line.get(md).toArray(new String[line.get(md).size()]);

                            // Remove authority unless the md is not authority controlled
                            if (!isAuthorityControlledField(md)) {
                                for (int i = 0; i < fromCSV.length; i++) {
                                    int pos = fromCSV[i].indexOf(csv.getAuthoritySeparator());
                                    if (pos > -1) {
                                        fromCSV[i] = fromCSV[i].substring(0, pos);
                                    }
                                }
                            }

                            // Add all the values from the CSV line
                            add(fromCSV, md, whatHasChanged);
                        }
                    }

                    // Check it has an owning collection
                    List<String> collections = line.get("collection");
                    if (collections == null) {
                        throw new MetadataImportException(
                            "New items must have a 'collection' assigned in the form of a handle");
                    }

                    // Check collections are really collections
                    ArrayList<Collection> check = new ArrayList<Collection>();
                    Collection collection;
                    for (String handle : collections) {
                        try {
                            // Resolve the handle to the collection
                            collection = (Collection) handleService.resolveToObject(c, handle);

                            // Check it resolved OK
                            if (collection == null) {
                                throw new MetadataImportException(
                                    "'" + handle + "' is not a Collection! You must specify a valid collection for " +
                                        "new items");
                            }

                            // Check for duplicate
                            if (check.contains(collection)) {
                                throw new MetadataImportException(
                                    "Duplicate collection assignment detected in new item! " + handle);
                            } else {
                                check.add(collection);
                            }
                        } catch (Exception ex) {
                            throw new MetadataImportException(
                                "'" + handle + "' is not a Collection! You must specify a valid collection for new " +
                                    "items",
                                ex);
                        }
                    }

                    // Record the addition to collections
                    boolean first = true;
                    for (String handle : collections) {
                        Collection extra = (Collection) handleService.resolveToObject(c, handle);
                        if (first) {
                            whatHasChanged.setOwningCollection(extra);
                        } else {
                            whatHasChanged.registerNewMappedCollection(extra);
                        }
                        first = false;
                    }

                    // Create the new item?
                    if (change) {
                        // Create the item
                        String collectionHandle = line.get("collection").get(0);
                        collection = (Collection) handleService.resolveToObject(c, collectionHandle);
                        wsItem = workspaceItemService.create(c, collection, useTemplate);
                        item = wsItem.getItem();

                        // Add the metadata to the item
                        List<BulkEditMetadataValue> relationships = new LinkedList<>();
                        for (BulkEditMetadataValue dcv : whatHasChanged.getAdds()) {
                            if (StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName())) {

                                if (!StringUtils.equals(dcv.getElement(), "type")) {
                                    relationships.add(dcv);
                                } else {
                                    handleRelationshipMetadataValueFromBulkEditMetadataValue(item, dcv);
                                }

                            } else {
                                itemService.addMetadata(c, item, dcv.getSchema(),
                                                        dcv.getElement(),
                                                        dcv.getQualifier(),
                                                        dcv.getLanguage(),
                                                        dcv.getValue(),
                                                        dcv.getAuthority(),
                                                        dcv.getConfidence());
                            }
                        }

                        for (BulkEditMetadataValue relationship : relationships) {
                            handleRelationshipMetadataValueFromBulkEditMetadataValue(item, relationship);
                        }
                        // Should the workflow be used?
                        if (useWorkflow) {
                            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
                            if (workflowNotify) {
                                wfItem = workflowService.start(c, wsItem);
                            } else {
                                wfItem = workflowService.startWithoutNotify(c, wsItem);
                            }
                        } else {
                            // Install the item
                            installItemService.installItem(c, wsItem);
                        }

                        // Add to extra collections
                        if (line.get("collection").size() > 0) {
                            for (int i = 1; i < collections.size(); i++) {
                                String handle = collections.get(i);
                                Collection extra = (Collection) handleService.resolveToObject(c, handle);
                                collectionService.addItem(c, extra, item);
                            }
                        }

                        // Commit changes to the object
//                        c.commit();
                        whatHasChanged.setItem(item);
                    }

                    // Record the changes
                    changes.add(whatHasChanged);
                }

                if (change) {
                    //only clear cache if changes have been made.
                    c.uncacheEntity(wsItem);
                    c.uncacheEntity(wfItem);
                    c.uncacheEntity(item);
                }
                populateRefAndRowMap(line, rowCount, item == null ? null : item.getID());
                // keep track of current rows processed
                rowCount++;
            }

            c.setMode(originalMode);
        } catch (MetadataImportException mie) {
            throw mie;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return the changes
        return changes;
    }


    /**
     * This metod handles the BulkEditMetadataValue objects that correspond to Relationship metadatavalues
     * @param item  The item to which this metadatavalue will belong
     * @param dcv   The BulkEditMetadataValue to be processed
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void handleRelationshipMetadataValueFromBulkEditMetadataValue(Item item, BulkEditMetadataValue dcv)
        throws SQLException, AuthorizeException, MetadataImportException {
        LinkedList<String> values = new LinkedList<>();
        values.add(dcv.getValue());
        LinkedList<String> authorities = new LinkedList<>();
        authorities.add(dcv.getAuthority());
        LinkedList<Integer> confidences = new LinkedList<>();
        confidences.add(dcv.getConfidence());
        handleRelationMetadata(c, item, dcv.getSchema(), dcv.getElement(),
                               dcv.getQualifier(),
                               dcv.getLanguage(), values, authorities, confidences);
    }

    /**
     * Compare an item metadata with a line from CSV, and optionally update the item
     *
     * @param item    The current item metadata
     * @param fromCSV The metadata from the CSV file
     * @param change  Whether or not to make the update
     * @param md      The element to compare
     * @param changes The changes object to populate
     * @param line    line in CSV file
     * @throws SQLException       if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException if there is an authorization problem with permissions
     */
    protected void compare(Item item, String[] fromCSV, boolean change,
                           String md, BulkEditChange changes, DSpaceCSVLine line)
        throws SQLException, AuthorizeException, MetadataImportException {
        // Log what metadata element we're looking at
        String all = "";
        for (String part : fromCSV) {
            all += part + ",";
        }
        all = all.substring(0, all.length());
        log.debug(LogManager.getHeader(c, "metadata_import",
                                       "item_id=" + item.getID() + ",fromCSV=" + all));

        // Don't compare collections or actions or rowNames
        if (("collection".equals(md)) || ("action".equals(md)) || ("rowName".equals(md))) {
            return;
        }

        // Make a String array of the current values stored in this element
        // First, strip of language if it is there
        String language = null;
        if (md.contains("[")) {
            String[] bits = md.split("\\[");
            language = bits[1].substring(0, bits[1].length() - 1);
        }

        AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);
        if (md.indexOf(':') > 0) {
            md = md.substring(md.indexOf(':') + 1);
        }

        String[] bits = md.split("\\.");
        String schema = bits[0];
        String element = bits[1];
        // If there is a language on the element, strip if off
        if (element.contains("[")) {
            element = element.substring(0, element.indexOf('['));
        }
        String qualifier = null;
        if (bits.length > 2) {
            qualifier = bits[2];

            // If there is a language, strip if off
            if (qualifier.contains("[")) {
                qualifier = qualifier.substring(0, qualifier.indexOf('['));
            }
        }
        log.debug(LogManager.getHeader(c, "metadata_import",
                                       "item_id=" + item.getID() + ",fromCSV=" + all +
                                           ",looking_for_schema=" + schema +
                                           ",looking_for_element=" + element +
                                           ",looking_for_qualifier=" + qualifier +
                                           ",looking_for_language=" + language));
        String[] dcvalues;
        if (fromAuthority == null) {
            List<MetadataValue> current = itemService.getMetadata(item, schema, element, qualifier, language);
            dcvalues = new String[current.size()];
            int i = 0;
            for (MetadataValue dcv : current) {
                if (dcv.getAuthority() == null || !isAuthorityControlledField(md)) {
                    dcvalues[i] = dcv.getValue();
                } else {
                    dcvalues[i] = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority();
                    dcvalues[i] += csv.getAuthoritySeparator() + (dcv.getConfidence() != -1 ? dcv
                        .getConfidence() : Choices.CF_ACCEPTED);
                }
                i++;
                log.debug(LogManager.getHeader(c, "metadata_import",
                                               "item_id=" + item.getID() + ",fromCSV=" + all +
                                                   ",found=" + dcv.getValue()));
            }
        } else {
            dcvalues = line.get(md).toArray(new String[line.get(md).size()]);
        }


        // Compare from current->csv
        for (int v = 0; v < fromCSV.length; v++) {
            String value = fromCSV[v];
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(language, schema, element, qualifier, value,
                                                                fromAuthority);
            if (fromAuthority != null) {
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv
                    .getAuthoritySeparator() + dcv.getConfidence();
                fromCSV[v] = value;
            }

            if ((value != null) && (!"".equals(value)) && (!contains(value, dcvalues))) {
                changes.registerAdd(dcv);
            } else {
                // Keep it
                changes.registerConstant(dcv);
            }
        }

        // Compare from csv->current
        for (String value : dcvalues) {
            // Look to see if it should be removed
            BulkEditMetadataValue dcv = new BulkEditMetadataValue();
            dcv.setSchema(schema);
            dcv.setElement(element);
            dcv.setQualifier(qualifier);
            dcv.setLanguage(language);
            if (value == null || !value.contains(csv.getAuthoritySeparator())) {
                simplyCopyValue(value, dcv);
            } else {
                String[] parts = value.split(csv.getAuthoritySeparator());
                dcv.setValue(parts[0]);
                dcv.setAuthority(parts[1]);
                dcv.setConfidence((parts.length > 2 ? Integer.valueOf(parts[2]) : Choices.CF_ACCEPTED));
            }

            // fromAuthority==null: with the current implementation metadata values from external authority sources
            // can only be used to add metadata, not to change or remove them
            // because e.g. an author that is not in the column "ORCID:dc.contributor.author" could still be in the
            // column "dc.contributor.author" so don't remove it
            if ((value != null) && (!"".equals(value)) && (!contains(value, fromCSV)) && fromAuthority == null) {
                // Remove it
                log.debug(LogManager.getHeader(c, "metadata_import",
                                               "item_id=" + item.getID() + ",fromCSV=" + all +
                                                   ",removing_schema=" + schema +
                                                   ",removing_element=" + element +
                                                   ",removing_qualifier=" + qualifier +
                                                   ",removing_language=" + language));
                changes.registerRemove(dcv);
            }
        }

        // Update the item if it has changed
        if ((change) &&
            ((changes.getAdds().size() > 0) || (changes.getRemoves().size() > 0))) {
            // Get the complete list of what values should now be in that element
            List<BulkEditMetadataValue> list = changes.getComplete();
            List<String> values = new ArrayList<String>();
            List<String> authorities = new ArrayList<String>();
            List<Integer> confidences = new ArrayList<Integer>();
            for (BulkEditMetadataValue value : list) {
                if ((qualifier == null) && (language == null)) {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (value.getQualifier() == null) &&
                        (value.getLanguage() == null)) {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                } else if (qualifier == null) {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (language.equals(value.getLanguage())) &&
                        (value.getQualifier() == null)) {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                } else if (language == null) {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (qualifier.equals(value.getQualifier())) &&
                        (value.getLanguage() == null)) {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                } else {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (qualifier.equals(value.getQualifier())) &&
                        (language.equals(value.getLanguage()))) {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                }
            }


            if (StringUtils.equals(schema, MetadataSchemaEnum.RELATION.getName())) {
                List<RelationshipType> relationshipTypeList = relationshipTypeService
                    .findByLeftwardOrRightwardTypeName(c, element);
                for (RelationshipType relationshipType : relationshipTypeList) {
                    for (Relationship relationship : relationshipService
                        .findByItemAndRelationshipType(c, item, relationshipType)) {
                        relationshipService.delete(c, relationship);
                        relationshipService.update(c, relationship);
                    }
                }
                handleRelationMetadata(c, item, schema, element, qualifier, language, values, authorities, confidences);
            } else {
                itemService.clearMetadata(c, item, schema, element, qualifier, language);
                itemService.addMetadata(c, item, schema, element, qualifier,
                                        language, values, authorities, confidences);
                itemService.update(c, item);
            }
        }
    }

    /**
     * This method decides whether the metadatavalue is of type relation.type or if it corresponds to
     * a relationship and handles it accordingly to their respective methods
     * @param c             The relevant DSpace context
     * @param item          The item to which this metadatavalue belongs to
     * @param schema        The schema for the metadatavalue
     * @param element       The element for the metadatavalue
     * @param qualifier     The qualifier for the metadatavalue
     * @param language      The language for the metadatavalue
     * @param values        The values for the metadatavalue
     * @param authorities   The authorities for the metadatavalue
     * @param confidences   The confidences for the metadatavalue
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void handleRelationMetadata(Context c, Item item, String schema, String element, String qualifier,
                                        String language, List<String> values, List<String> authorities,
                                        List<Integer> confidences) throws SQLException, AuthorizeException,
                                        MetadataImportException {

        if (StringUtils.equals(element, "type") && StringUtils.isBlank(qualifier)) {
            handleRelationTypeMetadata(c, item, schema, element, qualifier, language, values, authorities, confidences);

        } else {
            for (String value : values) {
                handleRelationOtherMetadata(c, item, element, value);
            }
        }

    }

    /**
     * Gets an existing entity from a reference.
     *
     * @param context the context to use.
     * @param reference the reference which may be a UUID, metadata reference, or rowName reference.
     * @return the entity, which is guaranteed to exist.
     * @throws MetadataImportException if the reference is badly formed or refers to a non-existing item.
     */
    private Entity getEntity(Context context, String reference) throws MetadataImportException {
        Entity entity = null;
        UUID uuid = resolveEntityRef(context, reference);
        // At this point, we have a uuid, so we can get an entity
        try {
            entity = entityService.findByItemId(context, uuid);
            if (entity.getItem() == null) {
                throw new IllegalArgumentException("No item found in repository with uuid: " + uuid);
            }
            return entity;
        } catch (SQLException sqle) {
            throw new MetadataImportException("Unable to find entity using reference: " + reference, sqle);
        }
    }

    /**
     * This method takes the item, element and values to determine what relationships should be built
     * for these parameters and calls on the method to construct them
     * @param c         The relevant DSpace context
     * @param item      The item that the relationships will be made for
     * @param element   The string determining which relationshiptype is to be used
     * @param value    The value for the relationship
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void handleRelationOtherMetadata(Context c, Item item, String element, String value)
        throws SQLException, AuthorizeException, MetadataImportException {
        if (value.isEmpty()) {
            return;
        }
        Entity entity = entityService.findByItemId(c, item.getID());
        boolean left = false;
        List<RelationshipType> acceptableRelationshipTypes = new LinkedList<>();

        Entity relationEntity = getEntity(c, value);

        List<RelationshipType> leftRelationshipTypesForEntity = entityService.getLeftRelationshipTypes(c, entity);
        List<RelationshipType> rightRelationshipTypesForEntity = entityService.getRightRelationshipTypes(c, entity);

        //Identify which RelationshipType objects match the combination of:
        // * the left entity type
        // * the right entity type
        // * the name of the relationship type (based on the expected direction of the relationship)
        //The matches are included in the acceptableRelationshipTypes
        for (RelationshipType relationshipType : entityService.getAllRelationshipTypes(c, entity)) {
            if (StringUtils.equalsIgnoreCase(relationshipType.getLeftwardType(), element)) {
                left = verifyValidLeftwardRelationshipType(c, entity, relationEntity, left,
                                                                             acceptableRelationshipTypes,
                                                                             leftRelationshipTypesForEntity,
                                                                             relationshipType);
            } else if (StringUtils.equalsIgnoreCase(relationshipType.getRightwardType(), element)) {
                left = verifyValidRightwardRelationshipType(c, entity, relationEntity, left,
                                                                          acceptableRelationshipTypes,
                                                                          rightRelationshipTypesForEntity,
                                                                          relationshipType);
            }
        }

        if (acceptableRelationshipTypes.size() > 1) {
            log.error("Ambiguous relationship_types were found");
            return;
        }
        if (acceptableRelationshipTypes.size() == 0) {
            log.error("no relationship_types were found");
            return;
        }

        //There is exactly one
        buildRelationObject(c, item, relationEntity.getItem(), left, acceptableRelationshipTypes.get(0));
    }

    /**
     * This method creates the relationship for the item and stores it in the database
     * @param c         The relevant DSpace context
     * @param item      The item for which this relationship will be constructed
     * @param otherItem    The item for the relationship
     * @param left      A boolean indicating whether the item is the leftItem or the rightItem
     * @param acceptedRelationshipType   The acceptable relationship type
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void buildRelationObject(Context c, Item item, Item otherItem, boolean left,
                                     RelationshipType acceptedRelationshipType)
        throws SQLException, AuthorizeException {
        Item leftItem = null;
        Item rightItem = null;
        if (left) {
            leftItem = item;
            rightItem = otherItem;
        } else {
            rightItem = item;
            leftItem = otherItem;
        }
        RelationshipType relationshipType = acceptedRelationshipType;
        int leftPlace = relationshipService.findLeftPlaceByLeftItem(c, leftItem) + 1;
        int rightPlace = relationshipService.findRightPlaceByRightItem(c, rightItem) + 1;
        Relationship persistedRelationship = relationshipService.create(c, leftItem, rightItem,
                                                                        relationshipType, leftPlace, rightPlace);
        relationshipService.update(c, persistedRelationship);
    }

    /**
     * This method will add RelationshipType objects to the acceptableRelationshipTypes list
     * if applicable and valid RelationshipType objects are found. It will also return a boolean indicating
     * whether we're dealing with a left Relationship or not
     * @param c                                 The relevant DSpace context
     * @param entity                            The Entity for which the RelationshipType has to be checked
     * @param relationEntity                    The other Entity of the Relationship
     * @param left                              Boolean indicating whether the Relationship is left or not
     * @param acceptableRelationshipTypes       The list of RelationshipType objects that will be added to
     * @param rightRelationshipTypesForEntity   The list of RelationshipType objects that are possible
     *                                          for the right entity
     * @param relationshipType                  The RelationshipType object that we want to check whether it's
     *                                          valid to be added or not
     * @return                                  A boolean indicating whether the relationship is left or right.
     *                                          Will be set to false if the relationship is valid.
     *                                          Will remain unmodified from the left parameter if the
     *                                          relationship is not valid.
     * @throws SQLException                     If something goes wrong
     */
    private boolean verifyValidRightwardRelationshipType(Context c, Entity entity,
                                                         Entity relationEntity,
                                                         boolean left,
                                                         List<RelationshipType>
                                                                           acceptableRelationshipTypes,
                                                         List<RelationshipType>
                                                                           rightRelationshipTypesForEntity,
                                                         RelationshipType relationshipType)
        throws SQLException {
        if (StringUtils.equalsIgnoreCase(entityService.getType(c, entity).getLabel(),
                                         relationshipType.getRightType().getLabel()) &&
            StringUtils.equalsIgnoreCase(entityService.getType(c, relationEntity).getLabel(),
                                         relationshipType.getLeftType().getLabel())) {

            for (RelationshipType rightRelationshipType : rightRelationshipTypesForEntity) {
                if (StringUtils.equalsIgnoreCase(rightRelationshipType.getLeftType().getLabel(),
                                                 relationshipType.getLeftType().getLabel()) ||
                    StringUtils.equalsIgnoreCase(rightRelationshipType.getRightType().getLabel(),
                                                 relationshipType.getLeftType().getLabel())) {
                    left = false;
                    acceptableRelationshipTypes.add(relationshipType);
                }
            }
        }
        return left;
    }

    /**
     * This method will add RelationshipType objects to the acceptableRelationshipTypes list
     * if applicable and valid RelationshipType objects are found. It will also return a boolean indicating
     * whether we're dealing with a left Relationship or not
     * @param c                                 The relevant DSpace context
     * @param entity                            The Entity for which the RelationshipType has to be checked
     * @param relationEntity                    The other Entity of the Relationship
     * @param left                              Boolean indicating whether the Relationship is left or not
     * @param acceptableRelationshipTypes       The list of RelationshipType objects that will be added to
     * @param leftRelationshipTypesForEntity    The list of RelationshipType objects that are possible
     *                                          for the left entity
     * @param relationshipType                  The RelationshipType object that we want to check whether it's
     *                                          valid to be added or not
     * @return                                  A boolean indicating whether the relationship is left or right.
     *                                          Will be set to true if the relationship is valid.
     *                                          Will remain unmodified from the left parameter if the
     *                                          relationship is not valid.
     * @throws SQLException                     If something goes wrong
     */
    private boolean verifyValidLeftwardRelationshipType(Context c, Entity entity,
                                                        Entity relationEntity,
                                                        boolean left,
                                                        List<RelationshipType>
                                                                              acceptableRelationshipTypes,
                                                        List<RelationshipType>
                                                                              leftRelationshipTypesForEntity,
                                                        RelationshipType relationshipType)
        throws SQLException {
        if (StringUtils.equalsIgnoreCase(entityService.getType(c, entity).getLabel(),
                                         relationshipType.getLeftType().getLabel()) &&
            StringUtils.equalsIgnoreCase(entityService.getType(c, relationEntity).getLabel(),
                                         relationshipType.getRightType().getLabel())) {
            for (RelationshipType leftRelationshipType : leftRelationshipTypesForEntity) {
                if (StringUtils.equalsIgnoreCase(leftRelationshipType.getRightType().getLabel(),
                                                 relationshipType.getRightType().getLabel()) ||
                    StringUtils.equalsIgnoreCase(leftRelationshipType.getLeftType().getLabel(),
                                                 relationshipType.getRightType().getLabel())) {
                    left = true;
                    acceptableRelationshipTypes.add(relationshipType);
                }
            }
        }
        return left;
    }

    /**
     * This method will add the relationship.type metadata to the item if an EntityType can be found for the value in
     * the values list.
     * @param c             The relevant DSpace context
     * @param item          The item to which this metadatavalue will be added
     * @param schema        The schema for the metadatavalue to be added
     * @param element       The element for the metadatavalue to be added
     * @param qualifier     The qualifier for the metadatavalue to be added
     * @param language      The language for the metadatavalue to be added
     * @param values        The value on which we'll search for EntityType object and it's the value
     *                      for the metadatavalue that will be created
     * @param authorities   The authority for the metadatavalue. This will be filled with the ID
     *                      of the found EntityType for the value if it exists
     * @param confidences   The confidence for the metadatavalue
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void handleRelationTypeMetadata(Context c, Item item, String schema, String element, String qualifier,
                                            String language, List<String> values, List<String> authorities,
                                            List<Integer> confidences)
        throws SQLException, AuthorizeException {
        EntityType entityType = entityTypeService.findByEntityType(c, values.get(0));
        if (entityType != null) {
            authorities.add(String.valueOf(entityType.getID()));
            itemService.clearMetadata(c, item, schema, element, qualifier, language);
            itemService.addMetadata(c, item, schema, element, qualifier, language,
                                    values, authorities, confidences);
            itemService.update(c, item);
        }
    }

    /**
     * Compare changes between an items owning collection and mapped collections
     * and what is in the CSV file
     *
     * @param item              The item in question
     * @param collections       The collection handles from the CSV file
     * @param actualCollections The Collections from the actual item
     * @param bechange          The bulkedit change object for this item
     * @param change            Whether or not to actuate a change
     * @throws SQLException            if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException      if there is an authorization problem with permissions
     * @throws IOException             Can be thrown when moving items in communities
     * @throws MetadataImportException If something goes wrong to be reported back to the user
     */
    protected void compare(Item item,
                           List<String> collections,
                           List<Collection> actualCollections,
                           BulkEditChange bechange,
                           boolean change)
        throws SQLException, AuthorizeException, IOException, MetadataImportException {
        // First, check the owning collection (as opposed to mapped collections) is the same of changed
        String oldOwner = item.getOwningCollection().getHandle();
        String newOwner = collections.get(0);
        // Resolve the handle to the collection
        Collection newCollection = (Collection) handleService.resolveToObject(c, newOwner);

        // Check it resolved OK
        if (newCollection == null) {
            throw new MetadataImportException(
                "'" + newOwner + "' is not a Collection! You must specify a valid collection ID");
        }

        if (!oldOwner.equals(newOwner)) {
            // Register the old and new owning collections
            bechange.changeOwningCollection(item.getOwningCollection(),
                                            (Collection) handleService.resolveToObject(c, newOwner));
        }

        // Second, loop through the strings from the CSV of mapped collections
        boolean first = true;
        for (String csvcollection : collections) {
            // Ignore the first collection as this is the owning collection
            if (!first) {
                // Look for it in the actual list of Collections
                boolean found = false;
                for (Collection collection : actualCollections) {
                    if (collection.getID() != item.getOwningCollection().getID()) {
                        // Is it there?
                        if (csvcollection.equals(collection.getHandle())) {
                            found = true;
                        }
                    }
                }

                // Was it found?
                DSpaceObject dso = handleService.resolveToObject(c, csvcollection);
                if ((dso == null) || (dso.getType() != Constants.COLLECTION)) {
                    throw new MetadataImportException("Collection defined for item " + item.getID() +
                                                          " (" + item.getHandle() + ") is not a collection");
                }
                if (!found) {
                    // Register the new mapped collection
                    Collection col = (Collection) dso;
                    bechange.registerNewMappedCollection(col);
                }
            }
            first = false;
        }

        // Third, loop through the strings from the current item
        for (Collection collection : actualCollections) {
            // Look for it in the actual list of Collections
            boolean found = false;
            first = true;
            for (String csvcollection : collections) {
                // Don't check the owning collection
                if ((first) && (collection.getID().equals(item.getOwningCollection().getID()))) {
                    found = true;
                } else {
                    // Is it there?
                    if (!first && collection.getHandle().equals(csvcollection)) {
                        found = true;
                    }
                }
                first = false;
            }

            // Was it found?
            if (!found) {
                // Record that it isn't there any more
                bechange.registerOldMappedCollection(collection);
            }
        }

        // Process the changes
        if (change) {
            // Remove old mapped collections
            for (Collection collection : bechange.getOldMappedCollections()) {
                collectionService.removeItem(c, collection, item);
            }

            // Add to new owned collection
            if (bechange.getNewOwningCollection() != null) {
                collectionService.addItem(c, bechange.getNewOwningCollection(), item);
                item.setOwningCollection(bechange.getNewOwningCollection());
                itemService.update(c, item);
            }

            // Remove from old owned collection (if still a member)
            if (bechange.getOldOwningCollection() != null) {
                boolean found = false;
                for (Collection c : item.getCollections()) {
                    if (c.getID().equals(bechange.getOldOwningCollection().getID())) {
                        found = true;
                    }
                }

                if (found) {
                    collectionService.removeItem(c, bechange.getOldOwningCollection(), item);
                }
            }

            // Add to new mapped collections
            for (Collection collection : bechange.getNewMappedCollections()) {
                collectionService.addItem(c, collection, item);
            }

        }
    }

    /**
     * Add an item metadata with a line from CSV, and optionally update the item
     *
     * @param fromCSV The metadata from the CSV file
     * @param md      The element to compare
     * @param changes The changes object to populate
     * @throws SQLException       when an SQL error has occurred (querying DSpace)
     * @throws AuthorizeException If the user can't make the changes
     */
    protected void add(String[] fromCSV, String md, BulkEditChange changes)
        throws SQLException, AuthorizeException {
        // Don't add owning collection or action
        if (("collection".equals(md)) || ("action".equals(md))) {
            return;
        }

        // Make a String array of the values
        // First, strip of language if it is there
        String language = null;
        if (md.contains("[")) {
            String[] bits = md.split("\\[");
            language = bits[1].substring(0, bits[1].length() - 1);
        }
        AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);
        if (md.indexOf(':') > 0) {
            md = md.substring(md.indexOf(':') + 1);
        }

        String[] bits = md.split("\\.");
        String schema = bits[0];
        String element = bits[1];
        // If there is a language on the element, strip if off
        if (element.contains("[")) {
            element = element.substring(0, element.indexOf('['));
        }
        String qualifier = null;
        if (bits.length > 2) {
            qualifier = bits[2];

            // If there is a language, strip if off
            if (qualifier.contains("[")) {
                qualifier = qualifier.substring(0, qualifier.indexOf('['));
            }
        }

        // Add all the values
        for (String value : fromCSV) {
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(language, schema, element, qualifier, value,
                                                                fromAuthority);
            if (fromAuthority != null) {
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv
                    .getAuthoritySeparator() + dcv.getConfidence();
            }

            // Add it
            if ((value != null) && (!"".equals(value))) {
                changes.registerAdd(dcv);
            }
        }
    }

    protected BulkEditMetadataValue getBulkEditValueFromCSV(String language, String schema, String element,
                                                            String qualifier, String value,
                                                            AuthorityValue fromAuthority) {
        // Look to see if it should be removed
        BulkEditMetadataValue dcv = new BulkEditMetadataValue();
        dcv.setSchema(schema);
        dcv.setElement(element);
        dcv.setQualifier(qualifier);
        dcv.setLanguage(language);
        if (fromAuthority != null) {
            if (value.indexOf(':') > 0) {
                value = value.substring(0, value.indexOf(':'));
            }

            // look up the value and authority in solr
            List<AuthorityValue> byValue = authorityValueService.findByValue(c, schema, element, qualifier, value);
            AuthorityValue authorityValue = null;
            if (byValue.isEmpty()) {
                String toGenerate = fromAuthority.generateString() + value;
                String field = schema + "_" + element + (StringUtils.isNotBlank(qualifier) ? "_" + qualifier : "");
                authorityValue = authorityValueService.generate(c, toGenerate, value, field);
                dcv.setAuthority(toGenerate);
            } else {
                authorityValue = byValue.get(0);
                dcv.setAuthority(authorityValue.getId());
            }

            dcv.setValue(authorityValue.getValue());
            dcv.setConfidence(Choices.CF_ACCEPTED);
        } else if (value == null || !value.contains(csv.getAuthoritySeparator())) {
            simplyCopyValue(value, dcv);
        } else {
            String[] parts = value.split(csv.getEscapedAuthoritySeparator());
            dcv.setValue(parts[0]);
            dcv.setAuthority(parts[1]);
            dcv.setConfidence((parts.length > 2 ? Integer.valueOf(parts[2]) : Choices.CF_ACCEPTED));
        }
        return dcv;
    }

    protected void simplyCopyValue(String value, BulkEditMetadataValue dcv) {
        dcv.setValue(value);
        dcv.setAuthority(null);
        dcv.setConfidence(Choices.CF_UNSET);
    }

    /**
     * Method to find if a String occurs in an array of Strings
     *
     * @param needle   The String to look for
     * @param haystack The array of Strings to search through
     * @return Whether or not it is contained
     */
    protected boolean contains(String needle, String[] haystack) {
        // Look for the needle in the haystack
        for (String examine : haystack) {
            if (clean(examine).equals(clean(needle))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clean elements before comparing
     *
     * @param in The element to clean
     * @return The cleaned up element
     */
    protected String clean(String in) {
        // Check for nulls
        if (in == null) {
            return null;
        }

        // Remove newlines as different operating systems sometimes use different formats
        return in.replaceAll("\r\n", "").replaceAll("\n", "").trim();
    }

    /**
     * Print the help message
     *
     * @param options  The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode) {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("MetatadataImport\n", options);
        System.out.println("\nmetadataimport: MetadataImport -f filename");
        System.exit(exitCode);
    }

    /**
     * Display the changes that have been detected, or that have been made
     *
     * @param changes The changes detected
     * @param changed Whether or not the changes have been made
     * @return The number of items that have changed
     */
    private static int displayChanges(List<BulkEditChange> changes, boolean changed) {
        // Display the changes
        int changeCounter = 0;
        for (BulkEditChange change : changes) {
            // Get the changes
            List<BulkEditMetadataValue> adds = change.getAdds();
            List<BulkEditMetadataValue> removes = change.getRemoves();
            List<Collection> newCollections = change.getNewMappedCollections();
            List<Collection> oldCollections = change.getOldMappedCollections();
            if ((adds.size() > 0) || (removes.size() > 0) ||
                (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null) ||
                (change.isDeleted()) || (change.isWithdrawn()) || (change.isReinstated())) {
                // Show the item
                Item i = change.getItem();

                System.out.println("-----------------------------------------------------------");
                if (!change.isNewItem()) {
                    System.out.println("Changes for item: " + i.getID() + " (" + i.getHandle() + ")");
                } else {
                    System.out.print("New item: ");
                    if (i != null) {
                        if (i.getHandle() != null) {
                            System.out.print(i.getID() + " (" + i.getHandle() + ")");
                        } else {
                            System.out.print(i.getID() + " (in workflow)");
                        }
                    }
                    System.out.println();
                }
                changeCounter++;
            }

            // Show actions
            if (change.isDeleted()) {
                if (changed) {
                    System.out.println(" - EXPUNGED!");
                } else {
                    System.out.println(" - EXPUNGE!");
                }
            }
            if (change.isWithdrawn()) {
                if (changed) {
                    System.out.println(" - WITHDRAWN!");
                } else {
                    System.out.println(" - WITHDRAW!");
                }
            }
            if (change.isReinstated()) {
                if (changed) {
                    System.out.println(" - REINSTATED!");
                } else {
                    System.out.println(" - REINSTATE!");
                }
            }

            if (change.getNewOwningCollection() != null) {
                Collection c = change.getNewOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        System.out.print(" + New owning collection (" + cHandle + "): ");
                    } else {
                        System.out.print(" + New owning collection  (" + cHandle + "): ");
                    }
                    System.out.println(cName);
                }

                c = change.getOldOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        System.out.print(" + Old owning collection (" + cHandle + "): ");
                    } else {
                        System.out.print(" + Old owning collection  (" + cHandle + "): ");
                    }
                    System.out.println(cName);
                }
            }

            // Show new mapped collections
            for (Collection c : newCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    System.out.print(" + Map to collection (" + cHandle + "): ");
                } else {
                    System.out.print(" + Mapped to collection  (" + cHandle + "): ");
                }
                System.out.println(cName);
            }

            // Show old mapped collections
            for (Collection c : oldCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    System.out.print(" + Un-map from collection (" + cHandle + "): ");
                } else {
                    System.out.print(" + Un-mapped from collection  (" + cHandle + "): ");
                }
                System.out.println(cName);
            }

            // Show additions
            for (BulkEditMetadataValue metadataValue : adds) {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null) {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null) {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed) {
                    System.out.print(" + Add    (" + md + "): ");
                } else {
                    System.out.print(" + Added   (" + md + "): ");
                }
                System.out.print(metadataValue.getValue());
                if (isAuthorityControlledField(md)) {
                    System.out.print(", authority = " + metadataValue.getAuthority());
                    System.out.print(", confidence = " + metadataValue.getConfidence());
                }
                System.out.println("");
            }

            // Show removals
            for (BulkEditMetadataValue metadataValue : removes) {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null) {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null) {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed) {
                    System.out.print(" - Remove (" + md + "): ");
                } else {
                    System.out.print(" - Removed (" + md + "): ");
                }
                System.out.print(metadataValue.getValue());
                if (isAuthorityControlledField(md)) {
                    System.out.print(", authority = " + metadataValue.getAuthority());
                    System.out.print(", confidence = " + metadataValue.getConfidence());
                }
                System.out.println("");
            }
        }
        return changeCounter;
    }

    /**
     * is the field is defined as authority controlled
     */
    private static boolean isAuthorityControlledField(String md) {
        String mdf = StringUtils.substringAfter(md, ":");
        mdf = StringUtils.substringBefore(mdf, "[");
        return authorityControlled.contains(mdf);
    }

    /**
     * Set authority controlled fields
     */
    private static void setAuthorizedMetadataFields() {
        authorityControlled = new HashSet<String>();
        Enumeration propertyNames = ConfigurationManager.getProperties().propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = ((String) propertyNames.nextElement()).trim();
            if (key.startsWith(AC_PREFIX)
                && ConfigurationManager.getBooleanProperty(key, false)) {
                authorityControlled.add(key.substring(AC_PREFIX.length()));
            }
        }
    }

    /**
     * main method to run the metadata exporter
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
        // Create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("f", "file", true, "source file");
        options.addOption("e", "email", true, "email address or user id of user (required if adding new items)");
        options.addOption("s", "silent", false,
                          "silent operation - doesn't request confirmation of changes USE WITH CAUTION");
        options.addOption("w", "workflow", false, "workflow - when adding new items, use collection workflow");
        options.addOption("n", "notify", false,
                          "notify - when adding new items using a workflow, send notification emails");
        options.addOption("t", "template", false,
                          "template - when adding new items, use the collection template (if it exists)");
        options.addOption("v", "validate-only", false,
                          "validate - just validate the csv, don't run the import");
        options.addOption("h", "help", false, "help");

        // Parse the command line arguments
        CommandLine line;
        try {
            line = parser.parse(options, argv);
        } catch (ParseException pe) {
            System.err.println("Error parsing command line arguments: " + pe.getMessage());
            System.exit(1);
            return;
        }

        if (line.hasOption('h')) {
            printHelp(options, 0);
        }

        // Check a filename is given
        if (!line.hasOption('f')) {
            System.err.println("Required parameter -f missing!");
            printHelp(options, 1);
        }
        String filename = line.getOptionValue('f');

        // Option to apply template to new items
        boolean useTemplate = false;
        if (line.hasOption('t')) {
            useTemplate = true;
        }

        // Options for workflows, and workflow notifications for new items
        boolean useWorkflow = false;
        boolean workflowNotify = false;
        if (line.hasOption('w')) {
            useWorkflow = true;
            if (line.hasOption('n')) {
                workflowNotify = true;
            }
        } else if (line.hasOption('n')) {
            System.err.println("Invalid option 'n': (notify) can only be specified with the 'w' (workflow) option.");
            System.exit(1);
        }

        // Create a context
        Context c;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
        } catch (Exception e) {
            System.err.println("Unable to create a new DSpace Context: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Find the EPerson, assign to context
        try {
            if (line.hasOption('e')) {
                EPerson eperson;
                String e = line.getOptionValue('e');
                if (e.indexOf('@') != -1) {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(c, e);
                } else {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().find(c, UUID.fromString(e));
                }

                if (eperson == null) {
                    System.out.println("Error, eperson cannot be found: " + e);
                    System.exit(1);
                }
                c.setCurrentUser(eperson);
            }
        } catch (Exception e) {
            System.err.println("Unable to find DSpace user: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Is this a silent run?
        boolean change = false;

        // Read lines from the CSV file
        DSpaceCSV csv;
        try {
            csv = new DSpaceCSV(new File(filename), c);
        } catch (MetadataImportInvalidHeadingException miihe) {
            System.err.println(miihe.getMessage());
            System.exit(1);
            return;
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Perform the first import - just highlight differences
        MetadataImport importer = new MetadataImport(c, csv);
        List<BulkEditChange> changes;

        boolean validateOnly = line.hasOption('v');

        if (!line.hasOption('s') || validateOnly) {
            // See what has changed
            try {
                changes = importer.runImport(false, useWorkflow, workflowNotify, useTemplate);
            } catch (MetadataImportException mie) {
                System.err.println("Error: " + mie.getMessage());
                System.exit(1);
                return;
            }

            // Display the changes
            int changeCounter = displayChanges(changes, false);

            // If there were changes, ask if we should execute them
            if (!validateOnly && changeCounter > 0) {
                try {
                    // Ask the user if they want to make the changes
                    System.out.println("\n" + changeCounter + " item(s) will be changed\n");
                    System.out.print("Do you want to make these changes? [y/n] ");
                    String yn = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                    if ("y".equalsIgnoreCase(yn)) {
                        change = true;
                    } else {
                        System.out.println("No data has been changed.");
                    }
                } catch (IOException ioe) {
                    System.err.println("Error: " + ioe.getMessage());
                    System.err.println("No changes have been made");
                    System.exit(1);
                }
            } else {
                System.out.println("There were no changes detected");
            }
        } else {
            change = true;
        }

        try {
            // If required, make the change
            if (change && !validateOnly) {
                try {
                    // Make the changes
                    changes = importer.runImport(true, useWorkflow, workflowNotify, useTemplate);
                } catch (MetadataImportException mie) {
                    System.err.println("Error: " + mie.getMessage());
                    System.exit(1);
                    return;
                }

                // Display the changes
                displayChanges(changes, true);

                // Commit the change to the DB
//                c.commit();
            }

            // Finsh off and tidy up
            c.restoreAuthSystemState();
            c.complete();
        } catch (Exception e) {
            c.abort();
            System.err.println("Error committing changes to database: " + e.getMessage());
            System.err.println("Aborting most recent changes.");
            System.exit(1);
        }
    }

    /**
     * Gets a copy of the given csv line with all entity references resolved to UUID strings.
     * Keys being iterated over represent metadatafields or special columns to be processed.
     *
     * @param line the csv line to process.
     * @return a copy, with all references resolved.
     * @throws MetadataImportException if there is an error resolving any entity reference.
     */
    public DSpaceCSVLine resolveEntityRefs(DSpaceCSVLine line) throws MetadataImportException {
        DSpaceCSVLine newLine = new DSpaceCSVLine(line.getID());
        for (String key : line.keys()) {
            // If a key represents a relation field attempt to resolve the reference from the csvRefMap
            if (key.split("\\.")[0].equalsIgnoreCase("relation")) {
                if (line.get(key).size() > 0) {
                    for (String val : line.get(key)) {
                        // Attempt to resolve the relation reference
                        // These can be a UUID, metadata reference or rowName reference
                        String uuid = resolveEntityRef(c, val).toString();
                        newLine.add(key, uuid);
                    }
                }
            } else {
                if (line.get(key).size() > 1) {
                    for (String value : line.get(key)) {
                        newLine.add(key, value);
                    }
                } else {
                    if (line.get(key).size() > 0) {
                        newLine.add(key, line.get(key).get(0));
                    }
                }
            }
        }
        return newLine;
    }

    /**
     * Populates the csvRefMap and csvRowMap for the given csv line.
     *
     * The csvRefMap is an index that keeps track of which rows have a specific value for
     * a specific metadata field or the special "rowName" column. This is used to help resolve indirect
     * entity references in the same CSV.
     *
     * The csvRowMap is a row number to UUID map, and contains an entry for every row that has
     * been processed so far which has a known (minted) UUID for its item. This is used to help complete
     * the resolution after the row number has been determined.
     *
     * @param line the csv line.
     * @param rowNumber the row number.
     * @param uuid the uuid of the item, which may be null if it has not been minted yet.
     */
    private void populateRefAndRowMap(DSpaceCSVLine line, int rowNumber, @Nullable UUID uuid) {
        if (uuid != null) {
            csvRowMap.put(rowNumber, uuid);
        }
        for (String key : line.keys()) {
            if (key.contains(".") && !key.split("\\.")[0].equalsIgnoreCase("relation") ||
                    key.equalsIgnoreCase("rowName")) {
                for (String value : line.get(key)) {
                    String valueKey = key + ":" + value;
                    Set<Integer> rowNums = csvRefMap.get(valueKey);
                    if (rowNums == null) {
                        rowNums = new HashSet<>();
                        csvRefMap.put(valueKey, rowNums);
                    }
                    rowNums.add(rowNumber);
                }
            }
        }
    }

    /**
     * Gets the UUID of the item indicated by the given reference, which may be a direct UUID string, a row reference
     * of the form rowName:VALUE, or a metadata value reference of the form schema.element[.qualifier]:VALUE.
     *
     * The reference may refer to a previously-processed item in the CSV or an item in the database.
     *
     * @param context the context to use.
     * @param reference the reference which may be a UUID, metadata reference, or rowName reference.
     * @return the uuid.
     * @throws MetadataImportException if the reference is malformed or ambiguous (refers to multiple items).
     */
    private UUID resolveEntityRef(Context context, String reference) throws MetadataImportException {
        // value reference
        UUID uuid = null;
        if (!reference.contains(":")) {
            // assume it's a UUID
            try {
                return UUID.fromString(reference);
            } catch (IllegalArgumentException e) {
                throw new MetadataImportException("Not a UUID or indirect entity reference: '" + reference + "'");
            }
        } else if (!reference.startsWith("rowName:") ) { // Not a rowName ref; so it's a metadata value reference
            MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
            MetadataFieldService metadataFieldService =
                    ContentServiceFactory.getInstance().getMetadataFieldService();
            int i = reference.indexOf(":");
            String mfValue = reference.substring(i + 1);
            String mf[] = reference.substring(0, i).split("\\.");
            if (mf.length < 2) {
                throw new MetadataImportException("Bad metadata field in reference: '" + reference
                        + "' (expected syntax is schema.element[.qualifier])");
            }
            String schema = mf[0];
            String element = mf[1];
            String qualifier = mf.length == 2 ? null : mf[2];
            try {
                MetadataField mfo = metadataFieldService.findByElement(context, schema, element, qualifier);
                Iterator<MetadataValue> mdv = metadataValueService.findByFieldAndValue(context, mfo, mfValue);
                if (mdv.hasNext()) {
                    MetadataValue mdvVal = mdv.next();
                    uuid = mdvVal.getDSpaceObject().getID();
                    if (mdv.hasNext()) {
                        throw new MetadataImportException("Ambiguous reference; multiple matches in db: " + reference);
                    }
                }
            } catch (SQLException e) {
                throw new MetadataImportException("Error looking up item by metadata reference: " + reference, e);
            }
        }
        // Lookup UUIDs that may have already been processed into the csvRefMap
        // See populateRefAndRowMap() for how the csvRefMap is populated
        // See getMatchingCSVUUIDs() for how the reference param is sourced from the csvRefMap
        Set<UUID> csvUUIDs = getMatchingCSVUUIDs(reference);
        if (csvUUIDs.size() > 1) {
            throw new MetadataImportException("Ambiguous reference; multiple matches in csv: " + reference);
        } else if (csvUUIDs.size() == 1) {
            UUID csvUUID = csvUUIDs.iterator().next();
            if (csvUUID.equals(uuid)) {
                return uuid; // one match from csv and db (same item)
            } else if (uuid != null) {
                throw new MetadataImportException("Ambiguous reference; multiple matches in db and csv: " + reference);
            } else {
                return csvUUID; // one match from csv
            }
        } else { // size == 0; the reference does not exist throw an error
            if (uuid == null) {
                throw new MetadataImportException("No matches found for reference: " + reference
                        + ",  Keep in mind you can only reference entries that are listed before " +
                        "this one within the CSV.");
            } else {
                return uuid; // one match from db
            }
        }
    }

    /**
     * Gets the set of matching lines as UUIDs that have already been processed given a metadata value.
     *
     * @param mdValueRef the metadataValue reference to search for.
     * @return the set of matching lines as UUIDs.
     */
    private Set<UUID> getMatchingCSVUUIDs(String mdValueRef) {
        Set<UUID> set = new HashSet<>();
        if (csvRefMap.containsKey(mdValueRef)) {
            for (Integer rowNum : csvRefMap.get(mdValueRef)) {
                set.add(getUUIDForRow(rowNum));
            }
        }
        return set;
    }

    /**
     * Gets the UUID of the item of a given row in the CSV, if it has been minted.
     * If the UUID has not yet been minted, gets a UUID representation of the row
     * (a UUID whose numeric value equals the row number).
     *
     * @param rowNum the row number.
     * @return the UUID of the item
     */
    private UUID getUUIDForRow(int rowNum) {
        if (csvRowMap.containsKey(rowNum)) {
            return csvRowMap.get(rowNum);
        } else {
            return new UUID(0, rowNum);
        }
    }

}
