/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.dspace.authority.AuthorityValue;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import java.util.*;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Metadata importer to allow the batch import of metadata from a file
 *
 * @author Stuart Lewis
 */
public class MetadataImport
{
    /** The Context */
    Context c;

    /** The DSpaceCSV object we're processing */
    DSpaceCSV csv;

    /** The lines to import */
    List<DSpaceCSVLine> toImport;

    /** The authority controlled fields */
    protected static Set<String> authorityControlled;
    static
    {
        setAuthorizedMetadataFields();
    }

    /** The prefix of the authority controlled field */
    protected static final String AC_PREFIX = "authority.controlled.";

    /** Logger */
    protected static final Logger log = Logger.getLogger(MetadataImport.class);

    protected final AuthorityValueService authorityValueService;

    protected final ItemService itemService;
    protected final InstallItemService installItemService;
    protected final CollectionService collectionService;
    protected final HandleService handleService;
    protected final WorkspaceItemService workspaceItemService;

    /**
     * Create an instance of the metadata importer. Requires a context and an array of CSV lines
     * to examine.
     *
     * @param c The context
     * @param toImport An array of CSV lines to examine
     */
    public MetadataImport(Context c, DSpaceCSV toImport)
    {
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
    }

    /**
     * Run an import. The import can either be read-only to detect changes, or
     * can write changes as it goes.
     *
     * @param change Whether or not to write the changes to the database
     * @param useWorkflow Whether the workflows should be used when creating new items
     * @param workflowNotify If the workflows should be used, whether to send notifications or not
     * @param useTemplate Use collection template if create new item
     * @return An array of BulkEditChange elements representing the items that have changed
     *
     * @throws MetadataImportException if something goes wrong
     */
    public List<BulkEditChange> runImport(boolean change,
                                          boolean useWorkflow,
                                          boolean workflowNotify,
                                          boolean useTemplate) throws MetadataImportException
    {
        // Store the changes
        ArrayList<BulkEditChange> changes = new ArrayList<BulkEditChange>();

        // Make the changes
        try
        {
            Context.Mode originalMode = c.getCurrentMode();
            c.setMode(Context.Mode.BATCH_EDIT);

            // Process each change
            for (DSpaceCSVLine line : toImport)
            {
                // Get the DSpace item to compare with
                UUID id = line.getID();

                // Is there an action column?
                if (csv.hasActions() && (!"".equals(line.getAction())) && (id == null))
                {
                    throw new MetadataImportException("'action' not allowed for new items!");
                }

                WorkspaceItem wsItem = null;
                WorkflowItem wfItem = null;
                Item item = null;

                // Is this a new item?
                if (id != null)
                {
                    // Get the item
                    item = itemService.find(c, id);
                    if (item == null)
                    {
                        throw new MetadataImportException("Unknown item ID " + id);
                    }

                    // Record changes
                    BulkEditChange whatHasChanged = new BulkEditChange(item);

                    // Has it moved collection?
                    List<String> collections = line.get("collection");
                    if (collections != null)
                    {
                        // Sanity check we're not orphaning it
                        if (collections.size() == 0)
                        {
                            throw new MetadataImportException("Missing collection from item " + item.getHandle());
                        }
                        List<Collection> actualCollections = item.getCollections();
                        compare(item, collections, actualCollections, whatHasChanged, change);
                    }

                    // Iterate through each metadata element in the csv line
                    for (String md : line.keys())
                    {
                        // Get the values we already have
                        if (!"id".equals(md))
                        {
                            // Get the values from the CSV
                            String[] fromCSV = line.get(md).toArray(new String[line.get(md).size()]);
                            // Remove authority unless the md is not authority controlled
                            if (!isAuthorityControlledField(md))
                            { 
                                for (int i=0; i<fromCSV.length; i++)
                                {
                                    int pos = fromCSV[i].indexOf(csv.getAuthoritySeparator());
                                    if (pos > -1)
                                    {
                                        fromCSV[i] = fromCSV[i].substring(0, pos);
                                    }
                                }
                            }

                            // Compare
                            compare(item, fromCSV, change, md, whatHasChanged, line);
                        }
                    }

                    if (csv.hasActions())
                    {
                        // Perform the action
                        String action = line.getAction();
                        if ("".equals(action))
                        {
                            // Do nothing
                        }
                        else if ("expunge".equals(action))
                        {
                            // Does the configuration allow deletes?
                            if (!ConfigurationManager.getBooleanProperty("bulkedit", "allowexpunge", false))
                            {
                                throw new MetadataImportException("'expunge' action denied by configuration");
                            }

                            // Remove the item

							if (change) {
								itemService.delete(c, item);
							}
                            
                            whatHasChanged.setDeleted();
                        }
                        else if ("withdraw".equals(action))
                        {
                            // Withdraw the item
                            if (!item.isWithdrawn())
                            {
                                if (change)
                                {
                                    itemService.withdraw(c, item);
                                }
                                whatHasChanged.setWithdrawn();
                            }
                        }
                        else if ("reinstate".equals(action))
                        {
                            // Reinstate the item
                            if (item.isWithdrawn())
                            {
                                if (change)
                                {
                                    itemService.reinstate(c, item);
                                }
                                whatHasChanged.setReinstated();
                            }
                        }
                        else {
                            // Unknown action!
                            throw new MetadataImportException("Unknown action: " + action);
                        }
                    }

                    // Only record if changes have been made
                    if (whatHasChanged.hasChanges())
                    {
                        changes.add(whatHasChanged);
                    }
                }
                else
                {
                    // This is marked as a new item, so no need to compare

                    // First check a user is set, otherwise this can't happen
                    if (c.getCurrentUser() == null)
                    {
                        throw new MetadataImportException("When adding new items, a user must be specified with the -e option");
                    }

                    // Iterate through each metadata element in the csv line
                    BulkEditChange whatHasChanged = new BulkEditChange();
                    for (String md : line.keys())
                    {
                        // Get the values we already have
                        if (!"id".equals(md))
                        {
                            // Get the values from the CSV
                            String[] fromCSV = line.get(md).toArray(new String[line.get(md).size()]);

                            // Remove authority unless the md is not authority controlled
                            if (!isAuthorityControlledField(md))
                            { 
                                for (int i=0; i<fromCSV.length; i++)
                                {
                                    int pos = fromCSV[i].indexOf(csv.getAuthoritySeparator());
                                    if (pos > -1)
                                    {
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
                    if (collections == null)
                    {
                        throw new MetadataImportException("New items must have a 'collection' assigned in the form of a handle");
                    }

                    // Check collections are really collections
                    ArrayList<Collection> check = new ArrayList<Collection>();
                    Collection collection;
                    for (String handle : collections)
                    {
                        try
                        {
                            // Resolve the handle to the collection
                            collection = (Collection) handleService.resolveToObject(c, handle);

                            // Check it resolved OK
                            if (collection == null)
                            {
                                throw new MetadataImportException("'" + handle + "' is not a Collection! You must specify a valid collection for new items");
                            }

                            // Check for duplicate
                            if (check.contains(collection))
                            {
                                throw new MetadataImportException("Duplicate collection assignment detected in new item! " + handle);
                            }
                            else
                            {
                                check.add(collection);
                            }
                        }
                        catch (Exception ex)
                        {
                            throw new MetadataImportException("'" + handle + "' is not a Collection! You must specify a valid collection for new items", ex);
                        }
                    }

                    // Record the addition to collections
                    boolean first = true;
                    for (String handle : collections)
                    {
                        Collection extra = (Collection) handleService.resolveToObject(c, handle);
                        if (first)
                        {
                            whatHasChanged.setOwningCollection(extra);
                        }
                        else
                        {
                            whatHasChanged.registerNewMappedCollection(extra);
                        }
                        first = false;
                    }

                    // Create the new item?
                    if (change)
                    {
                        // Create the item
                        String collectionHandle = line.get("collection").get(0);
                        collection = (Collection) handleService.resolveToObject(c, collectionHandle);
                        wsItem = workspaceItemService.create(c, collection, useTemplate);
                        item = wsItem.getItem();

                        // Add the metadata to the item
                        for (BulkEditMetadataValue dcv : whatHasChanged.getAdds())
                        {
                            itemService.addMetadata(c, item, dcv.getSchema(),
                                             dcv.getElement(),
                                             dcv.getQualifier(),
                                             dcv.getLanguage(),
                                             dcv.getValue(),
                                             dcv.getAuthority(),
                                             dcv.getConfidence());
                        }

                        // Should the workflow be used?
                        if(useWorkflow){
                            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
                            if (workflowNotify) {
                                wfItem = workflowService.start(c, wsItem);
                            } else {
                                wfItem = workflowService.startWithoutNotify(c, wsItem);
                            }
                        }
                        else
                        {
                            // Install the item
                            installItemService.installItem(c, wsItem);
                        }

                        // Add to extra collections
                        if (line.get("collection").size() > 0)
                        {
                            for (int i = 1; i < collections.size(); i++)
                            {
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
            }

            c.setMode(originalMode);
        }
        catch (MetadataImportException mie)
        {
            throw mie;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Return the changes
        return changes;
    }

    /**
     * Compare an item metadata with a line from CSV, and optionally update the item
     *
     * @param item The current item metadata
     * @param fromCSV The metadata from the CSV file
     * @param change Whether or not to make the update
     * @param md The element to compare
     * @param changes The changes object to populate
     * @param line line in CSV file
     * @throws SQLException if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException if there is an authorization problem with permissions
     */
    protected void compare(Item item, String[] fromCSV, boolean change,
                         String md, BulkEditChange changes, DSpaceCSVLine line) throws SQLException, AuthorizeException
    {
        // Log what metadata element we're looking at
        String all = "";
        for (String part : fromCSV)
        {
            all += part + ",";
        }
        all = all.substring(0, all.length());
        log.debug(LogManager.getHeader(c, "metadata_import",
                                       "item_id=" + item.getID() + ",fromCSV=" + all));

        // Don't compare collections  or actions
        if (("collection".equals(md)) || ("action".equals(md)))
        {
            return;
        }

        // Make a String array of the current values stored in this element
        // First, strip of language if it is there
        String language = null;
        if (md.contains("["))
        {
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
        if (element.contains("["))
        {
            element = element.substring(0, element.indexOf('['));
        }
        String qualifier = null;
        if (bits.length > 2)
        {
            qualifier = bits[2];

            // If there is a language, strip if off
            if (qualifier.contains("["))
            {
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
        if(fromAuthority==null) {
            List<MetadataValue> current = itemService.getMetadata(item, schema, element, qualifier, language);
            dcvalues = new String[current.size()];
            int i = 0;
            for (MetadataValue dcv : current) {
                if (dcv.getAuthority() == null || !isAuthorityControlledField(md)) {
                    dcvalues[i] = dcv.getValue();
                } else {
                    dcvalues[i] = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority();
                    dcvalues[i] += csv.getAuthoritySeparator() + (dcv.getConfidence() != -1 ? dcv.getConfidence() : Choices.CF_ACCEPTED);
                }
                i++;
                log.debug(LogManager.getHeader(c, "metadata_import",
                        "item_id=" + item.getID() + ",fromCSV=" + all +
                                ",found=" + dcv.getValue()));
            }
        }else{
            dcvalues = line.get(md).toArray(new String[line.get(md).size()]);
        }


        // Compare from current->csv
        for (int v = 0; v < fromCSV.length; v++) {
            String value = fromCSV[v];
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(language, schema, element, qualifier, value, fromAuthority);
            if (fromAuthority!=null) {
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv.getAuthoritySeparator() + dcv.getConfidence();
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
        for (String value : dcvalues)
        {
            // Look to see if it should be removed
            BulkEditMetadataValue dcv = new BulkEditMetadataValue();
            dcv.setSchema(schema);
            dcv.setElement(element);
            dcv.setQualifier(qualifier);
            dcv.setLanguage(language);
            if (value == null || !value.contains(csv.getAuthoritySeparator()))
                simplyCopyValue(value, dcv);
            else
            {
                String[] parts = value.split(csv.getAuthoritySeparator());
                dcv.setValue(parts[0]);
                dcv.setAuthority(parts[1]);
                dcv.setConfidence((parts.length > 2 ? Integer.valueOf(parts[2]) : Choices.CF_ACCEPTED));
            }

            if ((value != null) && (!"".equals(value)) && (!contains(value, fromCSV)) && fromAuthority==null)
            // fromAuthority==null: with the current implementation metadata values from external authority sources can only be used to add metadata, not to change or remove them
            // because e.g. an author that is not in the column "ORCID:dc.contributor.author" could still be in the column "dc.contributor.author" so don't remove it
            {
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
            ((changes.getAdds().size() > 0) || (changes.getRemoves().size() > 0)))
        {
            // Get the complete list of what values should now be in that element
            List<BulkEditMetadataValue> list = changes.getComplete();
            List<String> values = new ArrayList<String>();
            List<String> authorities = new ArrayList<String>();
            List<Integer> confidences = new ArrayList<Integer>();
            for (BulkEditMetadataValue value : list)
            {
                if ((qualifier == null) && (language == null))
                {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                         (value.getQualifier() == null) &&
                         (value.getLanguage() == null))
                    {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                }
                else if (qualifier == null)
                {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (language.equals(value.getLanguage())) &&
                        (value.getQualifier() == null))
                    {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                }
                else if (language == null)
                {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (qualifier.equals(value.getQualifier())) &&
                        (value.getLanguage() == null))
                    {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                }
                else
                {
                    if ((schema.equals(value.getSchema())) &&
                        (element.equals(value.getElement())) &&
                        (qualifier.equals(value.getQualifier())) &&
                        (language.equals(value.getLanguage())))
                    {
                        values.add(value.getValue());
                        authorities.add(value.getAuthority());
                        confidences.add(value.getConfidence());
                    }
                }
            }

            // Set those values
            itemService.clearMetadata(c, item, schema, element, qualifier, language);
            itemService.addMetadata(c, item, schema, element, qualifier, language, values, authorities, confidences);
            itemService.update(c, item);
        }
    }

    /**
     * Compare changes between an items owning collection and mapped collections
     * and what is in the CSV file
     *
     * @param item The item in question
     * @param collections The collection handles from the CSV file
     * @param actualCollections The Collections from the actual item
     * @param bechange The bulkedit change object for this item
     * @param change Whether or not to actuate a change
     *
     * @throws SQLException if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException if there is an authorization problem with permissions
     * @throws IOException Can be thrown when moving items in communities
     * @throws MetadataImportException If something goes wrong to be reported back to the user
     */
    protected void compare(Item item,
                         List<String> collections,
                         List<Collection> actualCollections,
                         BulkEditChange bechange,
                         boolean change) throws SQLException, AuthorizeException, IOException, MetadataImportException
    {
        // First, check the owning collection (as opposed to mapped collections) is the same of changed
        String oldOwner = item.getOwningCollection().getHandle();
        String newOwner = collections.get(0);
        // Resolve the handle to the collection
        Collection newCollection = (Collection) handleService.resolveToObject(c, newOwner);

        // Check it resolved OK
        if (newCollection == null)
        {
            throw new MetadataImportException("'" + newOwner + "' is not a Collection! You must specify a valid collection ID");
        }

        if (!oldOwner.equals(newOwner))
        {
            // Register the old and new owning collections
            bechange.changeOwningCollection(item.getOwningCollection(), (Collection) handleService.resolveToObject(c, newOwner));
        }

        // Second, loop through the strings from the CSV of mapped collections
        boolean first = true;
        for (String csvcollection : collections)
        {
            // Ignore the first collection as this is the owning collection
            if (!first)
            {
                // Look for it in the actual list of Collections
                boolean found = false;
                for (Collection collection : actualCollections)
                {
                    if (collection.getID() != item.getOwningCollection().getID()) {
                        // Is it there?
                        if (csvcollection.equals(collection.getHandle()))
                        {
                            found = true;
                        }
                    }
                }

                // Was it found?
                DSpaceObject dso = handleService.resolveToObject(c, csvcollection);
                if ((dso == null) || (dso.getType() != Constants.COLLECTION))
                {
                    throw new MetadataImportException("Collection defined for item " + item.getID() +
                                                      " (" + item.getHandle() + ") is not a collection");
                }
                if (!found)
                {
                    // Register the new mapped collection
                    Collection col = (Collection)dso;
                    bechange.registerNewMappedCollection(col);
                }
            }
            first = false;
        }

        // Third, loop through the strings from the current item
        for (Collection collection : actualCollections)
        {
            // Look for it in the actual list of Collections
            boolean found = false;
            first = true;
            for (String csvcollection : collections)
            {
                // Don't check the owning collection
                if ((first) && (collection.getID().equals(item.getOwningCollection().getID())))
                {
                    found = true;
                }
                else
                {
                    // Is it there?
                    if (!first && collection.getHandle().equals(csvcollection))
                    {
                        found = true;
                    }
                }
                first = false;
            }

            // Was it found?
            if (!found)
            {
                // Record that it isn't there any more
                bechange.registerOldMappedCollection(collection);
            }
        }

        // Process the changes
        if (change)
        {
            // Remove old mapped collections
            for (Collection collection : bechange.getOldMappedCollections())
            {
                collectionService.removeItem(c, collection, item);
            }

            // Add to new owned collection
            if (bechange.getNewOwningCollection() != null)
            {
                collectionService.addItem(c, bechange.getNewOwningCollection(), item);
                item.setOwningCollection(bechange.getNewOwningCollection());
                itemService.update(c, item);
            }

            // Remove from old owned collection (if still a member)
            if (bechange.getOldOwningCollection() != null)
            {
                boolean found = false;
                for (Collection c : item.getCollections())
                {
                    if (c.getID().equals(bechange.getOldOwningCollection().getID()))
                    {
                        found = true;
                    }
                }

                if (found)
                {
                    collectionService.removeItem(c, bechange.getOldOwningCollection(), item);
                }
            }

            // Add to new mapped collections
            for (Collection collection : bechange.getNewMappedCollections())
            {
                collectionService.addItem(c, collection, item);
            }

        }
    }

    /**
     * Add an item metadata with a line from CSV, and optionally update the item
     *
     * @param fromCSV The metadata from the CSV file
     * @param md The element to compare
     * @param changes The changes object to populate
     *
     * @throws SQLException when an SQL error has occurred (querying DSpace)
     * @throws AuthorizeException If the user can't make the changes
     */
    protected void add(String[] fromCSV, String md, BulkEditChange changes)
                                            throws SQLException, AuthorizeException
    {
        // Don't add owning collection or action
        if (("collection".equals(md)) || ("action".equals(md)))
        {
            return;
        }

        // Make a String array of the values
        // First, strip of language if it is there
        String language = null;
        if (md.contains("["))
        {
            String[] bits = md.split("\\[");
            language = bits[1].substring(0, bits[1].length() - 1);
        }
        AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);
        if (md.indexOf(':') > 0) {
            md = md.substring(md.indexOf(':')+1);
        }

        String[] bits = md.split("\\.");
        String schema = bits[0];
        String element = bits[1];
        // If there is a language on the element, strip if off
        if (element.contains("["))
        {
            element = element.substring(0, element.indexOf('['));
        }
        String qualifier = null;
        if (bits.length > 2)
        {
            qualifier = bits[2];

            // If there is a language, strip if off
            if (qualifier.contains("["))
            {
                qualifier = qualifier.substring(0, qualifier.indexOf('['));
            }
        }

        // Add all the values
        for (String value : fromCSV)
        {
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(language, schema, element, qualifier, value, fromAuthority);
            if(fromAuthority!=null){
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv.getAuthoritySeparator() + dcv.getConfidence();
            }

            // Add it
            if ((value != null) && (!"".equals(value)))
            {
                changes.registerAdd(dcv);
            }
        }
    }

    protected BulkEditMetadataValue getBulkEditValueFromCSV(String language, String schema, String element, String qualifier, String value, AuthorityValue fromAuthority) {
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
     * @param needle The String to look for
     * @param haystack The array of Strings to search through
     * @return Whether or not it is contained
     */
    protected boolean contains(String needle, String[] haystack)
    {
        // Look for the needle in the haystack
        for (String examine : haystack)
        {
            if (clean(examine).equals(clean(needle)))
            {
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
    protected String clean(String in)
    {
        // Check for nulls
        if (in == null)
        {
            return null;
        }
        
        // Remove newlines as different operating systems sometimes use different formats
        return in.replaceAll("\r\n", "").replaceAll("\n", "").trim();
    }

    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
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
    private static int displayChanges(List<BulkEditChange> changes, boolean changed)
    {
        // Display the changes
        int changeCounter = 0;
        for (BulkEditChange change : changes)
        {
            // Get the changes
            List<BulkEditMetadataValue> adds = change.getAdds();
            List<BulkEditMetadataValue> removes = change.getRemoves();
            List<Collection> newCollections = change.getNewMappedCollections();
            List<Collection> oldCollections = change.getOldMappedCollections();
            if ((adds.size() > 0) || (removes.size() > 0) ||
                (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null) ||
                (change.isDeleted()) || (change.isWithdrawn()) || (change.isReinstated()))
            {
                // Show the item
                Item i = change.getItem();

                System.out.println("-----------------------------------------------------------");
                if (!change.isNewItem())
                {
                    System.out.println("Changes for item: " + i.getID() + " (" + i.getHandle() + ")");
                }
                else
                {
                    System.out.print("New item: ");
                    if (i != null)
                    {
                        if (i.getHandle() != null)
                        {
                            System.out.print(i.getID() + " (" + i.getHandle() + ")");
                        }
                        else
                        {
                            System.out.print(i.getID() + " (in workflow)");
                        }
                    }
                    System.out.println();
                }
                changeCounter++;
            }

            // Show actions
            if (change.isDeleted())
            {
                if (changed)
                {
                    System.out.println(" - EXPUNGED!");
                }
                else
                {
                    System.out.println(" - EXPUNGE!");
                }
            }
            if (change.isWithdrawn())
            {
                if (changed)
                {
                    System.out.println(" - WITHDRAWN!");
                }
                else
                {
                    System.out.println(" - WITHDRAW!");
                }
            }
            if (change.isReinstated())
            {
                if (changed)
                {
                    System.out.println(" - REINSTATED!");
                }
                else
                {
                    System.out.println(" - REINSTATE!");
                }
            }

            if (change.getNewOwningCollection() != null)
            {
                Collection c = change.getNewOwningCollection();
                if (c != null)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed)
                    {
                        System.out.print(" + New owning collection (" + cHandle + "): ");
                    }
                    else
                    {
                        System.out.print(" + New owning collection  (" + cHandle + "): ");
                    }
                    System.out.println(cName);
                }

                c = change.getOldOwningCollection();
                if (c != null)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed)
                    {
                        System.out.print(" + Old owning collection (" + cHandle + "): ");
                    }
                    else
                    {
                        System.out.print(" + Old owning collection  (" + cHandle + "): ");
                    }
                    System.out.println(cName);
                }
            }

            // Show new mapped collections
            for (Collection c : newCollections)
            {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed)
                {
                    System.out.print(" + Map to collection (" + cHandle + "): ");
                }
                else
                {
                    System.out.print(" + Mapped to collection  (" + cHandle + "): ");
                }
                System.out.println(cName);
            }

            // Show old mapped collections
            for (Collection c : oldCollections)
            {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed)
                {
                    System.out.print(" + Un-map from collection (" + cHandle + "): ");
                }
                else
                {
                    System.out.print(" + Un-mapped from collection  (" + cHandle + "): ");
                }
                System.out.println(cName);
            }

            // Show additions
            for (BulkEditMetadataValue metadataValue : adds)
            {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null)
                {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null)
                {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed)
                {
                    System.out.print(" + Add    (" + md + "): ");
                }
                else
                {
                    System.out.print(" + Added   (" + md + "): ");
                }
                System.out.print(metadataValue.getValue());
                if (isAuthorityControlledField(md))
                {
                    System.out.print(", authority = " + metadataValue.getAuthority());
                    System.out.print(", confidence = " + metadataValue.getConfidence());
                }
                System.out.println("");
            }

            // Show removals
            for (BulkEditMetadataValue metadataValue : removes)
            {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null)
                {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null)
                {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed)
                {
                    System.out.print(" - Remove (" + md + "): ");
                }
                else
                {
                    System.out.print(" - Removed (" + md + "): ");
                }
                System.out.print(metadataValue.getValue());
                if (isAuthorityControlledField(md))
                {
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
     *
     */
    private static boolean isAuthorityControlledField(String md)
    {
        String mdf = StringUtils.substringAfter(md, ":");
        mdf = StringUtils.substringBefore(mdf, "[");
        return authorityControlled.contains(mdf);
    }

    /**
     * Set authority controlled fields
     *
     */
    private static void setAuthorizedMetadataFields()
    {
        authorityControlled = new HashSet<String>();
        Enumeration propertyNames = ConfigurationManager.getProperties().propertyNames();
        while(propertyNames.hasMoreElements())
        {
            String key = ((String) propertyNames.nextElement()).trim();
            if (key.startsWith(AC_PREFIX)
            && ConfigurationManager.getBooleanProperty(key, false))
            {
                authorityControlled.add(key.substring(AC_PREFIX.length())); 
            }
        }
    }

    /**
	 * main method to run the metadata exporter
	 *
	 * @param argv the command line arguments given
	 */
    public static void main(String[] argv)
    {
        // Create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("f", "file", true, "source file");
        options.addOption("e", "email", true, "email address or user id of user (required if adding new items)");
        options.addOption("s", "silent", false, "silent operation - doesn't request confirmation of changes USE WITH CAUTION");
        options.addOption("w", "workflow", false, "workflow - when adding new items, use collection workflow");
        options.addOption("n", "notify", false, "notify - when adding new items using a workflow, send notification emails");
        options.addOption("t", "template", false, "template - when adding new items, use the collection template (if it exists)");        
        options.addOption("h", "help", false, "help");

        // Parse the command line arguments
        CommandLine line;
        try
        {
            line = parser.parse(options, argv);
        }
        catch (ParseException pe)
        {
            System.err.println("Error parsing command line arguments: " + pe.getMessage());
            System.exit(1);
            return;
        }

        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        // Check a filename is given
        if (!line.hasOption('f'))
        {
            System.err.println("Required parameter -f missing!");
            printHelp(options, 1);
        }
        String filename = line.getOptionValue('f');

        // Option to apply template to new items
        boolean useTemplate = false;
        if (line.hasOption('t'))
        {
            useTemplate = true;
        }

        // Options for workflows, and workflow notifications for new items
        boolean useWorkflow = false;
        boolean workflowNotify = false; 
        if (line.hasOption('w'))
        {
            useWorkflow = true;
            if (line.hasOption('n'))
            {
                workflowNotify = true;
            }
        }
        else if (line.hasOption('n'))
        {
            System.err.println("Invalid option 'n': (notify) can only be specified with the 'w' (workflow) option.");
            System.exit(1);
        }

        // Create a context
        Context c;
        try
        {
            c = new Context();
            c.turnOffAuthorisationSystem();
        }
        catch (Exception e)
        {
            System.err.println("Unable to create a new DSpace Context: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Find the EPerson, assign to context
        try
        {
            if (line.hasOption('e'))
            {
                EPerson eperson;
                String e = line.getOptionValue('e');
                if (e.indexOf('@') != -1)
                {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(c, e);
                } else
                {
                    eperson = EPersonServiceFactory.getInstance().getEPersonService().find(c, UUID.fromString(e));
                }

                if (eperson == null)
                {
                    System.out.println("Error, eperson cannot be found: " + e);
                    System.exit(1);
                }
                c.setCurrentUser(eperson);
            }
        } catch (Exception e)
        {
            System.err.println("Unable to find DSpace user: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Is this a silent run?
        boolean change = false;

        // Read lines from the CSV file
        DSpaceCSV csv;
        try
        {
            csv = new DSpaceCSV(new File(filename), c);
        }
        catch (MetadataImportInvalidHeadingException miihe)
        {
            System.err.println(miihe.getMessage());
            System.exit(1);
            return;
        }
        catch (Exception e)
        {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Perform the first import - just highlight differences
        MetadataImport importer = new MetadataImport(c, csv);
        List<BulkEditChange> changes;

        if (!line.hasOption('s'))
        {
            // See what has changed
            try
            {
                changes = importer.runImport(false, useWorkflow, workflowNotify, useTemplate);
            }
            catch (MetadataImportException mie)
            {
                System.err.println("Error: " + mie.getMessage());
                System.exit(1);
                return;
            }

            // Display the changes
            int changeCounter = displayChanges(changes, false);

            // If there were changes, ask if we should execute them
            if (changeCounter > 0)
            {
                try
                {
                    // Ask the user if they want to make the changes
                    System.out.println("\n" + changeCounter + " item(s) will be changed\n");
                    System.out.print("Do you want to make these changes? [y/n] ");
                    String yn = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                    if ("y".equalsIgnoreCase(yn))
                    {
                        change = true;
                    }
                    else
                    {
                        System.out.println("No data has been changed.");
                    }
                }
                catch (IOException ioe)
                {
                    System.err.println("Error: " + ioe.getMessage());
                    System.err.println("No changes have been made");
                    System.exit(1);
                }
            }
            else
            {
                System.out.println("There were no changes detected");
            }
        }
        else
        {
            change = true;
        }

        try
        {
            // If required, make the change
            if (change)
            {
                try
                {
                    // Make the changes
                    changes = importer.runImport(true, useWorkflow, workflowNotify, useTemplate);
                }
                catch (MetadataImportException mie)
                {
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
        }
        catch (Exception e)
        {
            c.abort();
            System.err.println("Error committing changes to database: " + e.getMessage());
            System.err.println("Aborting most recent changes.");
            System.exit(1);
        }
    }
}
