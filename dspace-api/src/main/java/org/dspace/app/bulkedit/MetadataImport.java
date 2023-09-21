/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.RelationshipUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Entity;
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
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Metadata importer to allow the batch import of metadata from a file
 *
 * @author Stuart Lewis
 */
public class MetadataImport extends DSpaceRunnable<MetadataImportScriptConfiguration> {

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

    /**
     * The prefix of the authority controlled field
     */
    protected static final String AC_PREFIX = "authority.controlled.";

    /**
     * Map of field:value to csv row number, used to resolve indirect entity target references.
     *
     * @see #populateRefAndRowMap(DSpaceCSVLine, UUID)
     */
    protected Map<String, Set<Integer>> csvRefMap = new HashMap<>();

    /**
     * Map of csv row number to UUID, used to resolve indirect entity target references.
     *
     * @see #populateRefAndRowMap(DSpaceCSVLine, UUID)
     */
    protected HashMap<Integer, UUID> csvRowMap = new HashMap<>();

    /**
     * Map of UUIDs to their entity types.
     *
     * @see #populateRefAndRowMap(DSpaceCSVLine, UUID)
     */
    protected HashMap<UUID, String> entityTypeMap = new HashMap<>();

    /**
     * Map of UUIDs to their relations that are referenced within any import with their referrers.
     *
     * @see #populateEntityRelationMap(String, String, String)
     */
    protected HashMap<String, HashMap<String, ArrayList<String>>> entityRelationMap = new HashMap<>();


    /**
     * Collection of errors generated during relation validation process.
     */
    protected ArrayList<String> relationValidationErrors = new ArrayList<>();

    /**
     * Counter of rows processed in a CSV.
     */
    protected Integer rowCount = 1;

    private boolean useTemplate = false;
    private String filename = null;
    private boolean useWorkflow = false;
    private boolean workflowNotify = false;
    private boolean change = false;
    private boolean help = false;
    protected boolean validateOnly;

    /**
     * Logger
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataImport.class);

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected EntityService entityService = ContentServiceFactory.getInstance().getEntityService();
    protected AuthorityValueService authorityValueService = AuthorityServiceFactory.getInstance()
                                                                                   .getAuthorityValueService();
    protected ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Create an instance of the metadata importer. Requires a context and an array of CSV lines
     * to examine.
     *
     * @param toImport An array of CSV lines to examine
     */
    public void initMetadataImport(DSpaceCSV toImport) {
        // Store the import settings
        this.toImport = toImport.getCSVLines();
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }
        // Create a context
        Context c = null;
        c = new Context();
        c.turnOffAuthorisationSystem();

        // Find the EPerson, assign to context
        assignCurrentUserInContext(c);

        if (authorityControlled == null) {
            setAuthorizedMetadataFields();
        }
        // Read commandLines from the CSV file
        try {

            Optional<InputStream> optionalFileStream = handler.getFileStream(c, filename);
            if (optionalFileStream.isPresent()) {
                csv = new DSpaceCSV(optionalFileStream.get(), c);
            } else {
                throw new IllegalArgumentException("Error reading file, the file couldn't be found for filename: " +
                                                       filename);
            }
        } catch (MetadataImportInvalidHeadingException miihe) {
            throw miihe;
        } catch (Exception e) {
            throw new Exception("Error reading file: " + e.getMessage(), e);
        }

        // Perform the first import - just highlight differences
        initMetadataImport(csv);
        List<BulkEditChange> changes;

        if (!commandLine.hasOption('s') || validateOnly) {
            // See what has changed
            try {
                changes = runImport(c, false, useWorkflow, workflowNotify, useTemplate);
            } catch (MetadataImportException mie) {
                throw mie;
            }

            // Display the changes
            int changeCounter = displayChanges(changes, false);

            // If there were changes, ask if we should execute them
            if (!validateOnly && changeCounter > 0) {
                try {
                    // Ask the user if they want to make the changes
                    handler.logInfo("\n" + changeCounter + " item(s) will be changed\n");
                    change = determineChange(handler);

                } catch (IOException ioe) {
                    throw new IOException("Error: " + ioe.getMessage() + ", No changes have been made", ioe);
                }
            } else {
                handler.logInfo("There were no changes detected");
            }
        } else {
            change = true;
        }

        try {
            // If required, make the change
            if (change && !validateOnly) {
                try {
                    // Make the changes
                    changes = runImport(c, true, useWorkflow, workflowNotify, useTemplate);
                } catch (MetadataImportException mie) {
                    throw mie;
                }

                // Display the changes
                displayChanges(changes, true);
            }

            // Finsh off and tidy up
            c.restoreAuthSystemState();
            c.complete();
        } catch (Exception e) {
            c.abort();
            throw new Exception(
                "Error committing changes to database: " + e.getMessage() + ", aborting most recent changes", e);
        }

    }

    protected void assignCurrentUserInContext(Context context) throws ParseException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            try {
                EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
                context.setCurrentUser(ePerson);
            } catch (SQLException e) {
                log.error("Something went wrong trying to fetch the eperson for uuid: " + uuid, e);
            }
        }
    }

    /**
     * This method determines whether the changes should be applied or not. This is default set to true for the REST
     * script as we don't want to interact with the caller. This will be overwritten in the CLI script to ask for
     * confirmation
     * @param handler   Applicable DSpaceRunnableHandler
     * @return boolean indicating the value
     * @throws IOException  If something goes wrong
     */
    protected boolean determineChange(DSpaceRunnableHandler handler) throws IOException {
        return true;
    }

    @Override
    public MetadataImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("metadata-import",
                                                                 MetadataImportScriptConfiguration.class);
    }


    public void setup() throws ParseException {
        useTemplate = false;
        filename = null;
        useWorkflow = false;
        workflowNotify = false;

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
        filename = commandLine.getOptionValue('f');

        // Option to apply template to new items
        if (commandLine.hasOption('t')) {
            useTemplate = true;
        }

        // Options for workflows, and workflow notifications for new items
        if (commandLine.hasOption('w')) {
            useWorkflow = true;
            if (commandLine.hasOption('n')) {
                workflowNotify = true;
            }
        } else if (commandLine.hasOption('n')) {
            throw new ParseException(
                "Invalid option 'n': (notify) can only be specified with the 'w' (workflow) option.");
        }
        validateOnly = commandLine.hasOption('v');

        // Is this a silent run?
        change = false;
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
     * @throws MetadataImportException  if something goes wrong
     */
    public List<BulkEditChange> runImport(Context c, boolean change,
                                          boolean useWorkflow,
                                          boolean workflowNotify,
                                          boolean useTemplate)
        throws MetadataImportException, SQLException, AuthorizeException, WorkflowException, IOException {
        // Store the changes
        ArrayList<BulkEditChange> changes = new ArrayList<BulkEditChange>();

        // Make the changes
        Context.Mode originalMode = c.getCurrentMode();
        c.setMode(Context.Mode.BATCH_EDIT);

        // Process each change
        rowCount = 1;
        for (DSpaceCSVLine line : toImport) {
            // Resolve target references to other items
            populateRefAndRowMap(line, line.getID());
            line = resolveEntityRefs(c, line);
            // Get the DSpace item to compare with
            UUID id = line.getID();

            // Is there an action column?
            if (csv.hasActions() && (!"".equals(line.getAction())) && (id == null)) {
                throw new MetadataImportException("'action' not allowed for new items!");
            }

            WorkspaceItem wsItem = null;
            WorkflowItem wfItem = null;
            Item item = null;

            // Is this an existing item?
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
                    compare(c, item, collections, actualCollections, whatHasChanged, change);
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
                        compareAndUpdate(c, item, fromCSV, change, md, whatHasChanged, line);
                    }
                }

                if (csv.hasActions()) {
                    // Perform the action
                    String action = line.getAction();
                    if ("".equals(action)) {
                        // Do nothing
                    } else if ("expunge".equals(action)) {
                        // Does the configuration allow deletes?
                        if (!configurationService.getBooleanProperty("bulkedit.allowexpunge", false)) {
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
                        add(c, fromCSV, md, whatHasChanged);
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
                    for (BulkEditMetadataValue dcv : whatHasChanged.getAdds()) {
                        if (!StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName())) {
                            itemService.addMetadata(c, item, dcv.getSchema(),
                                                    dcv.getElement(),
                                                    dcv.getQualifier(),
                                                    dcv.getLanguage(),
                                                    dcv.getValue(),
                                                    dcv.getAuthority(),
                                                    dcv.getConfidence());
                        }
                    }
                    //Add relations after all metadata has been processed
                    for (BulkEditMetadataValue dcv : whatHasChanged.getAdds()) {
                        if (StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName())) {
                            addRelationship(c, item, dcv.getElement(), dcv.getValue());
                        }
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

                    whatHasChanged.setItem(item);
                }

                // Record the changes
                changes.add(whatHasChanged);
            }

            if (change && (rowCount % configurationService.getIntProperty("bulkedit.change.commit.count", 100) == 0)) {
                c.commit();
                handler.logInfo(LogHelper.getHeader(c, "metadata_import_commit", "lineNumber=" + rowCount));
            }
            populateRefAndRowMap(line, item == null ? null : item.getID());
            // keep track of current rows processed
            rowCount++;
        }
        if (change) {
            c.commit();
        }

        c.setMode(Context.Mode.READ_ONLY);


        // Return the changes
        if (!change) {
            validateExpressedRelations(c);
        }
        return changes;
    }

    /**
     * Compare an item metadata with a line from CSV, and optionally update the item.
     *
     * @param item    The current item metadata
     * @param fromCSV The metadata from the CSV file
     * @param change  Whether or not to make the update
     * @param md      The element to compare
     * @param changes The changes object to populate
     * @param line    line in CSV file
     * @throws SQLException       if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException if there is an authorization problem with permissions
     * @throws MetadataImportException custom exception for error handling within metadataimport
     */
    protected void compareAndUpdate(Context c, Item item, String[] fromCSV, boolean change,
                                    String md, BulkEditChange changes, DSpaceCSVLine line)
        throws SQLException, AuthorizeException, MetadataImportException {
        // Log what metadata element we're looking at
        String all = "";
        for (String part : fromCSV) {
            all += part + ",";
        }
        all = all.substring(0, all.length());
        log.debug(LogHelper.getHeader(c, "metadata_import",
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
        log.debug(LogHelper.getHeader(c, "metadata_import",
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
                log.debug(LogHelper.getHeader(c, "metadata_import",
                                               "item_id=" + item.getID() + ",fromCSV=" + all +
                                                   ",found=" + dcv.getValue()));
            }
        } else {
            dcvalues = line.get(md).toArray(new String[line.get(md).size()]);
        }


        // Compare from current->csv
        for (int v = 0; v < fromCSV.length; v++) {
            String value = fromCSV[v];
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(c, language, schema, element, qualifier, value,
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
                log.debug(LogHelper.getHeader(c, "metadata_import",
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
                addRelationships(c, item, element, values);
            } else {
                itemService.clearMetadata(c, item, schema, element, qualifier, language);
                itemService.addMetadata(c, item, schema, element, qualifier,
                                        language, values, authorities, confidences);
                itemService.update(c, item);
            }
        }
    }

    /**
     *
     * Adds multiple relationships with a matching typeName to an item.
     *
     * @param c             The relevant DSpace context
     * @param item          The item to which this metadatavalue belongs to
     * @param typeName       The element for the metadatavalue
     * @param values to iterate over
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void addRelationships(Context c, Item item, String typeName, List<String> values)
        throws SQLException, AuthorizeException,
        MetadataImportException {
        for (String value : values) {
            addRelationship(c, item, typeName, value);
        }
    }

    /**
     * Gets an existing entity from a target reference.
     *
     * @param context the context to use.
     * @param targetReference the target reference which may be a UUID, metadata reference, or rowName reference.
     * @return the entity, which is guaranteed to exist.
     * @throws MetadataImportException if the target reference is badly formed or refers to a non-existing item.
     */
    private Entity getEntity(Context context, String targetReference) throws MetadataImportException {
        Entity entity = null;
        UUID uuid = resolveEntityRef(context, targetReference);
        // At this point, we have a uuid, so we can get an entity
        try {
            entity = entityService.findByItemId(context, uuid);
            if (entity.getItem() == null) {
                throw new IllegalArgumentException("No item found in repository with uuid: " + uuid);
            }
            return entity;
        } catch (SQLException sqle) {
            throw new MetadataImportException("Unable to find entity using reference: " + targetReference, sqle);
        }
    }

    /**
     *
     * Creates a relationship for the given item
     *
     * @param c         The relevant DSpace context
     * @param item      The item that the relationships will be made for
     * @param typeName     The relationship typeName
     * @param value    The value for the relationship
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void addRelationship(Context c, Item item, String typeName, String value)
        throws SQLException, AuthorizeException, MetadataImportException {
        if (value.isEmpty()) {
            return;
        }
        boolean left = false;

        // Get entity from target reference
        Entity relationEntity = getEntity(c, value);
        // Get relationship type of entity and item
        String relationEntityRelationshipType = itemService.getMetadata(relationEntity.getItem(),
                                                                        "dspace", "entity",
                                                                        "type", Item.ANY).get(0).getValue();
        String itemRelationshipType = itemService.getMetadata(item, "dspace", "entity",
                                                              "type", Item.ANY).get(0).getValue();

        // Get the correct RelationshipType based on typeName
        List<RelationshipType> relType = relationshipTypeService.findByLeftwardOrRightwardTypeName(c, typeName);
        RelationshipType foundRelationshipType = matchRelationshipType(relType,
                                                                       relationEntityRelationshipType,
                                                                       itemRelationshipType, typeName);

        if (foundRelationshipType == null) {
            throw new MetadataImportException("Error on CSV row " + rowCount + ":" + "\n" +
                                                  "No Relationship type found for:\n" +
                                                  "Target type: " + relationEntityRelationshipType + "\n" +
                                                  "Origin referer type: " + itemRelationshipType + "\n" +
                                                  "with typeName: " + typeName);
        }

        if (foundRelationshipType.getLeftwardType().equalsIgnoreCase(typeName)) {
            left = true;
        }

        // Placeholder items for relation placing
        Item leftItem = null;
        Item rightItem = null;
        if (left) {
            leftItem = item;
            rightItem = relationEntity.getItem();
        } else {
            leftItem = relationEntity.getItem();
            rightItem = item;
        }

        // Create the relationship, appending to the end
        Relationship persistedRelationship = relationshipService.create(
            c, leftItem, rightItem, foundRelationshipType, -1, -1
        );
        relationshipService.update(c, persistedRelationship);
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
    protected void compare(Context c, Item item,
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
                for (Collection collection : item.getCollections()) {
                    if (collection.getID().equals(bechange.getOldOwningCollection().getID())) {
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
    protected void add(Context c, String[] fromCSV, String md, BulkEditChange changes)
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
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(c, language, schema, element, qualifier, value,
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

    protected BulkEditMetadataValue getBulkEditValueFromCSV(Context c, String language, String schema, String element,
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
     * Display the changes that have been detected, or that have been made
     *
     * @param changes The changes detected
     * @param changed Whether or not the changes have been made
     * @return The number of items that have changed
     */
    private int displayChanges(List<BulkEditChange> changes, boolean changed) {
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
                handler.logInfo("-----------------------------------------------------------");
                if (!change.isNewItem()) {
                    handler.logInfo("Changes for item: " + i.getID() + " (" + i.getHandle() + ")");
                } else {
                    handler.logInfo("New item: ");
                    if (i != null) {
                        if (i.getHandle() != null) {
                            handler.logInfo(i.getID() + " (" + i.getHandle() + ")");
                        } else {
                            handler.logInfo(i.getID() + " (in workflow)");
                        }
                    }
                }
                changeCounter++;
            }

            // Show actions
            if (change.isDeleted()) {
                if (changed) {
                    handler.logInfo(" - EXPUNGED!");
                } else {
                    handler.logInfo(" - EXPUNGE!");
                }
            }
            if (change.isWithdrawn()) {
                if (changed) {
                    handler.logInfo(" - WITHDRAWN!");
                } else {
                    handler.logInfo(" - WITHDRAW!");
                }
            }
            if (change.isReinstated()) {
                if (changed) {
                    handler.logInfo(" - REINSTATED!");
                } else {
                    handler.logInfo(" - REINSTATE!");
                }
            }

            if (change.getNewOwningCollection() != null) {
                Collection c = change.getNewOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        handler.logInfo(" + New owning collection (" + cHandle + "): ");
                    } else {
                        handler.logInfo(" + New owning collection  (" + cHandle + "): ");
                    }
                    handler.logInfo(cName);
                }

                c = change.getOldOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        handler.logInfo(" + Old owning collection (" + cHandle + "): ");
                    } else {
                        handler.logInfo(" + Old owning collection  (" + cHandle + "): ");
                    }
                    handler.logInfo(cName);
                }
            }

            // Show new mapped collections
            for (Collection c : newCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    handler.logInfo(" + Map to collection (" + cHandle + "): ");
                } else {
                    handler.logInfo(" + Mapped to collection  (" + cHandle + "): ");
                }
                handler.logInfo(cName);
            }

            // Show old mapped collections
            for (Collection c : oldCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    handler.logInfo(" + Un-map from collection (" + cHandle + "): ");
                } else {
                    handler.logInfo(" + Un-mapped from collection  (" + cHandle + "): ");
                }
                handler.logInfo(cName);
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
                    handler.logInfo(" + Add    (" + md + "): ");
                } else {
                    handler.logInfo(" + Added   (" + md + "): ");
                }
                handler.logInfo(metadataValue.getValue());
                if (isAuthorityControlledField(md)) {
                    handler.logInfo(", authority = " + metadataValue.getAuthority());
                    handler.logInfo(", confidence = " + metadataValue.getConfidence());
                }
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
                    handler.logInfo(" - Remove (" + md + "): ");
                } else {
                    handler.logInfo(" - Removed (" + md + "): ");
                }
                handler.logInfo(metadataValue.getValue());
                if (isAuthorityControlledField(md)) {
                    handler.logInfo(", authority = " + metadataValue.getAuthority());
                    handler.logInfo(", confidence = " + metadataValue.getConfidence());
                }
            }
        }
        return changeCounter;
    }

    /**
     * is the field is defined as authority controlled
     */
    private static boolean isAuthorityControlledField(String md) {
        String mdf = md.contains(":") ? StringUtils.substringAfter(md, ":") : md;
        mdf = StringUtils.substringBefore(mdf, "[");
        return authorityControlled.contains(mdf);
    }

    /**
     * Set authority controlled fields
     */
    private void setAuthorizedMetadataFields() {
        authorityControlled = new HashSet<>();
        Enumeration propertyNames = configurationService.getProperties().propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = ((String) propertyNames.nextElement()).trim();
            if (key.startsWith(AC_PREFIX)
                && configurationService.getBooleanProperty(key, false)) {
                authorityControlled.add(key.substring(AC_PREFIX.length()));
            }
        }
    }

    /**
     * Gets a copy of the given csv line with all entity target references resolved to UUID strings.
     * Keys being iterated over represent metadatafields or special columns to be processed.
     *
     * @param line the csv line to process.
     * @return a copy, with all references resolved.
     * @throws MetadataImportException if there is an error resolving any entity target reference.
     */
    public DSpaceCSVLine resolveEntityRefs(Context c, DSpaceCSVLine line) throws MetadataImportException {
        DSpaceCSVLine newLine = new DSpaceCSVLine(line.getID());
        UUID originId = evaluateOriginId(line.getID());
        for (String key : line.keys()) {
            // If a key represents a relation field attempt to resolve the target reference from the csvRefMap
            if (key.split("\\.")[0].equalsIgnoreCase("relation")) {
                if (line.get(key).size() > 0) {
                    for (String val : line.get(key)) {
                        // Attempt to resolve the relation target reference
                        // These can be a UUID, metadata target reference or rowName target reference
                        String uuid = resolveEntityRef(c, val).toString();
                        newLine.add(key, uuid);
                        //Entity refs have been resolved / placeholdered
                        //Populate the EntityRelationMap
                        populateEntityRelationMap(uuid, key, originId.toString());
                    }
                } else {
                    newLine.add(key, null);
                }
            } else {
                if (line.get(key).size() > 0) {
                    for (String value : line.get(key)) {
                        newLine.add(key, value);
                    }
                } else {
                    newLine.add(key, null);
                }
            }
        }

        return newLine;
    }

    /**
     * Populate the entityRelationMap with all target references and it's asscoiated typeNames
     * to their respective origins
     *
     * @param refUUID the target reference UUID for the relation
     * @param relationField the field of the typeNames to relate from
     */
    private void populateEntityRelationMap(String refUUID, String relationField, String originId) {
        HashMap<String, ArrayList<String>> typeNames = null;
        if (entityRelationMap.get(refUUID) == null) {
            typeNames = new HashMap<>();
            ArrayList<String> originIds = new ArrayList<>();
            originIds.add(originId);
            typeNames.put(relationField, originIds);
            entityRelationMap.put(refUUID, typeNames);
        } else {
            typeNames = entityRelationMap.get(refUUID);
            if (typeNames.get(relationField) == null) {
                ArrayList<String> originIds = new ArrayList<>();
                originIds.add(originId);
                typeNames.put(relationField, originIds);
            } else {
                ArrayList<String> originIds = typeNames.get(relationField);
                originIds.add(originId);
                typeNames.put(relationField, originIds);
            }
            entityRelationMap.put(refUUID, typeNames);
        }
    }

    /**
     * Populates the csvRefMap, csvRowMap, and entityTypeMap for the given csv line.
     *
     * The csvRefMap is an index that keeps track of which rows have a specific value for
     * a specific metadata field or the special "rowName" column. This is used to help resolve indirect
     * entity target references in the same CSV.
     *
     * The csvRowMap is a row number to UUID map, and contains an entry for every row that has
     * been processed so far which has a known (minted) UUID for its item. This is used to help complete
     * the resolution after the row number has been determined.
     *
     * @param line the csv line.
     * @param uuid the uuid of the item, which may be null if it has not been minted yet.
     */
    private void populateRefAndRowMap(DSpaceCSVLine line, @Nullable UUID uuid) {
        if (uuid != null) {
            csvRowMap.put(rowCount, uuid);
        } else {
            csvRowMap.put(rowCount, new UUID(0, rowCount));
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
                    rowNums.add(rowCount);
                }
            }
            //Populate entityTypeMap
            if (key.equalsIgnoreCase("dspace.entity.type") && line.get(key).size() > 0) {
                if (uuid == null) {
                    entityTypeMap.put(new UUID(0, rowCount), line.get(key).get(0));
                } else {
                    entityTypeMap.put(uuid, line.get(key).get(0));
                }
            }
        }
    }

    /**
     * Gets the UUID of the item indicated by the given target reference,
     * which may be a direct UUID string, a row reference
     * of the form rowName:VALUE, or a metadata value reference of the form schema.element[.qualifier]:VALUE.
     *
     * The reference may refer to a previously-processed item in the CSV or an item in the database.
     *
     * @param context the context to use.
     * @param reference the target reference which may be a UUID, metadata reference, or rowName reference.
     * @return the uuid.
     * @throws MetadataImportException if the target reference is malformed or ambiguous (refers to multiple items).
     */
    private UUID resolveEntityRef(Context context, String reference) throws MetadataImportException {
        // value reference
        UUID uuid = null;
        if (!reference.contains(":")) {
            // assume it's a UUID
            try {
                return UUID.fromString(reference);
            } catch (IllegalArgumentException e) {
                throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                      "Not a UUID or indirect entity reference: '" + reference + "'");
            }
        }
        if (reference.contains("::virtual::")) {
            return UUID.fromString(StringUtils.substringBefore(reference, "::virtual::"));
        } else if (!reference.startsWith("rowName:")) { // Not a rowName ref; so it's a metadata value reference
            MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
            MetadataFieldService metadataFieldService =
                ContentServiceFactory.getInstance().getMetadataFieldService();
            int i = reference.indexOf(":");
            String mfValue = reference.substring(i + 1);
            String mf[] = reference.substring(0, i).split("\\.");
            if (mf.length < 2) {
                throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                      "Bad metadata field in reference: '" + reference
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
                        throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                          "Ambiguous reference; multiple matches in db: " + reference);
                    }
                }
            } catch (SQLException e) {
                throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                      "Error looking up item by metadata reference: " + reference, e);
            }
        }
        // Lookup UUIDs that may have already been processed into the csvRefMap
        // See populateRefAndRowMap() for how the csvRefMap is populated
        // See getMatchingCSVUUIDs() for how the reference param is sourced from the csvRefMap
        Set<UUID> csvUUIDs = getMatchingCSVUUIDs(reference);
        if (csvUUIDs.size() > 1) {
            throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                  "Ambiguous reference; multiple matches in csv: " + reference);
        } else if (csvUUIDs.size() == 1) {
            UUID csvUUID = csvUUIDs.iterator().next();
            if (csvUUID.equals(uuid)) {
                return uuid; // one match from csv and db (same item)
            } else if (uuid != null) {
                throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                  "Ambiguous reference; multiple matches in db and csv: " + reference);
            } else {
                return csvUUID; // one match from csv
            }
        } else { // size == 0; the reference does not exist throw an error
            if (uuid == null) {
                throw new MetadataImportException("Error in CSV row " + rowCount + ":\n" +
                                                      "No matches found for reference: " + reference
                                                      + "\nKeep in mind you can only reference entries that are " +
                                                      "listed before " +
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

    /**
     * Return a UUID of the origin in process or a placeholder for the origin to be evaluated later
     *
     * @param originId UUID of the origin
     * @return the UUID of the item or UUID placeholder
     */
    private UUID evaluateOriginId(@Nullable UUID originId) {
        if (originId != null) {
            return originId;
        } else {
            return new UUID(0, rowCount);
        }
    }

    /**
     * Validate every relation modification expressed in the CSV.
     *
     */
    private void validateExpressedRelations(Context c) throws MetadataImportException {
        for (String targetUUID : entityRelationMap.keySet()) {
            String targetType = null;
            try {
                // Get the type of reference. Attempt lookup in processed map first before looking in archive.
                if (entityTypeMap.get(UUID.fromString(targetUUID)) != null) {
                    targetType = entityTypeService.
                                                      findByEntityType(c,
                                                                       entityTypeMap.get(UUID.fromString(targetUUID)))
                                                  .getLabel();
                } else {
                    // Target item may be archived; check there.
                    // Add to errors if Realtionship.type cannot be derived
                    Item targetItem = null;
                    if (itemService.find(c, UUID.fromString(targetUUID)) != null) {
                        targetItem = itemService.find(c, UUID.fromString(targetUUID));
                        List<MetadataValue> relTypes = itemService.
                                                                      getMetadata(targetItem, "dspace", "entity",
                                                                                  "type", Item.ANY);
                        String relTypeValue = null;
                        if (relTypes.size() > 0) {
                            relTypeValue = relTypes.get(0).getValue();
                            targetType = entityTypeService.findByEntityType(c, relTypeValue).getLabel();
                        } else {
                            relationValidationErrors.add("Cannot resolve Entity type for target UUID: " +
                                                             targetUUID);
                        }
                    } else {
                        relationValidationErrors.add("Cannot resolve Entity type for target UUID: " +
                                                         targetUUID);
                    }
                }
                if (targetType == null) {
                    continue;
                }
                // Get typeNames for each origin referer of this target.
                for (String typeName : entityRelationMap.get(targetUUID).keySet()) {
                    // Resolve Entity Type for each origin referer.
                    for (String originRefererUUID : entityRelationMap.get(targetUUID).get(typeName)) {
                        // Evaluate row number for origin referer.
                        String originRow = "N/A";
                        if (csvRowMap.containsValue(UUID.fromString(originRefererUUID))) {
                            for (int key : csvRowMap.keySet()) {
                                if (csvRowMap.get(key).toString().equalsIgnoreCase(originRefererUUID)) {
                                    originRow = key + "";
                                    break;
                                }
                            }
                        }
                        String originType = "";
                        // Validate target type and origin type pairing with typeName or add to errors.
                        // Attempt lookup in processed map first before looking in archive.
                        if (entityTypeMap.get(UUID.fromString(originRefererUUID)) != null) {
                            originType = entityTypeMap.get(UUID.fromString(originRefererUUID));
                            validateTypesByTypeByTypeName(c, targetType, originType, typeName, originRow);
                        } else {
                            // Origin item may be archived; check there.
                            // Add to errors if Realtionship.type cannot be derived.
                            Item originItem = null;
                            if (itemService.find(c, UUID.fromString(targetUUID)) != null) {
                                DSpaceCSVLine dSpaceCSVLine = this.csv.getCSVLines()
                                                                      .get(Integer.valueOf(originRow) - 1);
                                List<String> relTypes = dSpaceCSVLine.get("dspace.entity.type");
                                if (relTypes == null || relTypes.isEmpty()) {
                                    dSpaceCSVLine.get("dspace.entity.type[]");
                                }

                                if (relTypes != null && relTypes.size() > 0) {
                                    String relTypeValue = relTypes.get(0);
                                    relTypeValue = StringUtils.remove(relTypeValue, "\"").trim();
                                    originType = entityTypeService.findByEntityType(c, relTypeValue).getLabel();
                                    validateTypesByTypeByTypeName(c, targetType, originType, typeName, originRow);
                                } else {
                                    originItem = itemService.find(c, UUID.fromString(originRefererUUID));
                                    if (originItem != null) {
                                        List<MetadataValue> mdv = itemService.getMetadata(originItem,
                                                                                          "dspace",
                                                                                          "entity", "type",
                                                                                          Item.ANY);
                                        if (!mdv.isEmpty()) {
                                            String relTypeValue = mdv.get(0).getValue();
                                            originType = entityTypeService.findByEntityType(c, relTypeValue).getLabel();
                                            validateTypesByTypeByTypeName(c, targetType, originType, typeName,
                                                                          originRow);
                                        } else {
                                            relationValidationErrors.add("Error on CSV row " + originRow + ":" + "\n" +
                                                     "Cannot resolve Entity type for reference: " + originRefererUUID);
                                        }
                                    } else {
                                        relationValidationErrors.add("Error on CSV row " + originRow + ":" + "\n" +
                                                                         "Cannot resolve Entity type for reference: "
                                                                         + originRefererUUID);
                                    }
                                }

                            } else {
                                relationValidationErrors.add("Error on CSV row " + originRow + ":" + "\n" +
                                                                 "Cannot resolve Entity type for reference: "
                                                                 + originRefererUUID + " in row: " + originRow);
                            }
                        }
                    }
                }

            } catch (SQLException sqle) {
                throw new MetadataImportException("Error interacting with database!", sqle);
            }

        } // If relationValidationErrors is empty all described relationships are valid.
        if (!relationValidationErrors.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (String error : relationValidationErrors) {
                errors.append(error + "\n");
            }
            throw new MetadataImportException("Error validating relationships: \n" + errors);
        }
    }

    /**
     * Generates a list of potential Relationship Types given a typeName and attempts to match the given
     * targetType and originType to a Relationship Type in the list.
     *
     * @param targetType entity type of target.
     * @param originType entity type of origin referrer.
     * @param typeName left or right typeName of the respective Relationship.
     * @return the UUID of the item.
     */
    private void validateTypesByTypeByTypeName(Context c,
                                               String targetType, String originType, String typeName, String originRow)
        throws MetadataImportException {
        try {
            RelationshipType foundRelationshipType = null;
            List<RelationshipType> relationshipTypeList = relationshipTypeService.
                                                                                     findByLeftwardOrRightwardTypeName(
                                                                                         c, typeName.split("\\.")[1]);
            // Validate described relationship form the CSV.
            foundRelationshipType = matchRelationshipType(relationshipTypeList, targetType, originType, typeName);
            if (foundRelationshipType == null) {
                relationValidationErrors.add("Error on CSV row " + originRow + ":" + "\n" +
                                                 "No Relationship type found for:\n" +
                                                 "Target type: " + targetType + "\n" +
                                                 "Origin referer type: " + originType + "\n" +
                                                 "with typeName: " + typeName + " for type: " + originType);
            }
        } catch (SQLException sqle) {
            throw new MetadataImportException("Error interacting with database!", sqle);
        }
    }

    /**
     * Matches two Entity types to a Relationship Type from a set of Relationship Types.
     *
     * @param relTypes set of Relationship Types.
     * @param targetType entity type of target.
     * @param originType entity type of origin referer.
     * @return null or matched Relationship Type.
     */
    private RelationshipType matchRelationshipType(List<RelationshipType> relTypes,
                                                   String targetType, String originType, String originTypeName) {
        return RelationshipUtils.matchRelationshipType(relTypes, targetType, originType, originTypeName);
    }

}
