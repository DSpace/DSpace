/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.BulkEditMetadataField;
import org.dspace.app.bulkedit.BulkEditMetadataValue;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.bulkedit.cache.CSVBulkEditCache;
import org.dspace.app.bulkedit.cache.CSVBulkEditCacheImpl;
import org.dspace.app.bulkedit.util.BulkEditUtil;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class CSVBulkEditRegisterServiceImpl implements BulkEditRegisterService<DSpaceCSV> {
    protected static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(CSVBulkEditRegisterServiceImpl.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected HandleService handleService;

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected BulkEditUtil csvBulkEditUtil;

    @Autowired
    protected AuthorityValueService authorityValueService;

    private CSVBulkEditCache bulkEditCache;

    public List<BulkEditChange> registerBulkEditChange(Context c, DSpaceCSV csv)
        throws MetadataImportException, SQLException, AuthorizeException, IOException {
        bulkEditCache = new CSVBulkEditCacheImpl();

        Context.Mode lastMode = c.getCurrentMode();
        // Force a READ ONLY mode to make it clear no actual changes are meant to be made during a register
        c.setMode(Context.Mode.READ_ONLY);
        bulkEditCache.resetCache();

        List<BulkEditChange> changes = new ArrayList<>();

        bulkEditCache.resetRowCount();
        for (DSpaceCSVLine line : csv.getCSVLines()) {
            // Resolve target references to other items
            line = resolveEntityRefs(c, line);

            // Get the DSpace item to compare with
            UUID id = line.getID();

            // Is there an action column?
            if ((!"".equals(line.getAction())) && (id == null)) {
                throw new MetadataImportException("'action' not allowed for new items!");
            }

            Map<String, List<String>> metadataValues =
                getAuthorityCleanMetadataValues(line, csv.getAuthoritySeparator());

            BulkEditChange whatHasChanged;

            // Is this an existing item?
            if (id != null) {
                // Get the item
                Item item = itemService.find(c, id);
                if (item == null) {
                    throw new MetadataImportException("Unknown item ID " + id);
                }

                // Record changes
                whatHasChanged = new BulkEditChange(item);

                // Has it moved collection?
                List<String> collections = line.get("collection");
                if (collections != null) {
                    // Sanity check we're not orphaning it
                    if (collections.isEmpty()) {
                        throw new MetadataImportException("Missing collection from item " + item.getHandle());
                    }
                    List<Collection> actualCollections = item.getCollections();
                    compareCollections(c, item, collections, actualCollections, whatHasChanged);
                }

                // Iterate through each metadata element in the csv line
                for (String md : metadataValues.keySet()) {
                    // Compare
                    compareMetadata(c, item, csv, metadataValues.get(md), md, whatHasChanged, line);
                }

                // Perform the action
                String action = line.getAction();
                if (!"".equals(action)) {
                    if ("expunge".equals(action)) {
                        // Does the configuration allow deletes?
                        if (!configurationService.getBooleanProperty("bulkedit.allowexpunge", false)) {
                            throw new MetadataImportException("'expunge' action denied by configuration");
                        }

                        // Remove the item
                        whatHasChanged.setDeleted();
                    } else if ("withdraw".equals(action)) {
                        // Withdraw the item
                        if (!item.isWithdrawn()) {
                            whatHasChanged.setWithdrawn();
                        }
                    } else if ("reinstate".equals(action)) {
                        // Reinstate the item
                        if (item.isWithdrawn()) {
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
                whatHasChanged = new BulkEditChange(new UUID(0, bulkEditCache.getRowCount()));
                for (String md : metadataValues.keySet()) {
                    // Add all the values from the CSV line
                    add(c, csv, metadataValues.get(md), md, whatHasChanged);
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

                // Record the changes
                changes.add(whatHasChanged);
            }

            bulkEditCache.populateReferenceMaps(line, bulkEditCache.getRowCount(), whatHasChanged.getUuid());
            bulkEditCache.increaseRowCount();
        }

        bulkEditCache.validateExpressedRelations(c, csv);

        // Restore the Context Mode
        c.setMode(lastMode);

        return changes;
    }

    /**
     * Compare changes between an items owning collection and mapped collections
     * and what is in the CSV file
     *
     * @param item              The item in question
     * @param collections       The collection handles from the CSV file
     * @param actualCollections The Collections from the actual item
     * @param bechange          The bulkedit change object for this item
     * @throws SQLException            if there is a problem accessing a Collection from the database, from its handle
     * @throws AuthorizeException      if there is an authorization problem with permissions
     * @throws IOException             Can be thrown when moving items in communities
     * @throws MetadataImportException If something goes wrong to be reported back to the user
     */
    protected void compareCollections(Context c, Item item, List<String> collections,
                                      List<Collection> actualCollections, BulkEditChange bechange)
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
        UUID originId = bulkEditCache.evaluateOriginId(line.getID());
        for (String key : line.keys()) {
            // If a key represents a relation field attempt to resolve the target reference from the csvRefMap
            if (key.split("\\.")[0].equalsIgnoreCase("relation")) {
                if (!line.get(key).isEmpty()) {
                    for (String val : line.get(key)) {
                        // Attempt to resolve the relation target reference
                        // These can be a UUID, metadata target reference or rowName target reference
                        String uuid = bulkEditCache.resolveEntityRef(c, val).toString();
                        newLine.add(key, uuid);
                        //Entity refs have been resolved / placeholdered
                        //Populate the EntityRelationMap
                        bulkEditCache.populateEntityRelationMap(uuid, key, originId.toString());
                    }
                } else {
                    newLine.add(key, null);
                }
            } else {
                if (!line.get(key).isEmpty()) {
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
     * Compare an item metadata with a line from CSV, and optionally update the item.
     *
     * @param item    The current item metadata
     * @param fromCSV The metadata from the CSV file
     * @param md      The element to compare
     * @param changes The changes object to populate
     * @param line    line in CSV file
     */
    protected void compareMetadata(Context c, Item item, DSpaceCSV csv, List<String> fromCSV,
                                   String md, BulkEditChange changes, DSpaceCSVLine line) {
        // Log what metadata element we're looking at
        String all = StringUtils.join(fromCSV, ',');
        log.debug(LogHelper.getHeader(c, "metadata_import",
            "item_id=" + item.getID() + ",fromCSV=" + all));

        BulkEditMetadataField metadataField = BulkEditMetadataField.parse(md);
        AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);

        log.debug(LogHelper.getHeader(c, "metadata_import",
            "item_id=" + item.getID() + ",fromCSV=" + all +
                ",looking_for_schema=" + metadataField.getSchema() +
                ",looking_for_element=" + metadataField.getElement() +
                ",looking_for_qualifier=" + metadataField.getQualifier() +
                ",looking_for_language=" + metadataField.getLanguage()));
        List<String> dcvalues = new ArrayList<>();
        if (fromAuthority == null) {
            List<MetadataValue> current = itemService.getMetadata(item, metadataField.getSchema(),
                metadataField.getElement(), metadataField.getQualifier(), metadataField.getLanguage(), false);
            for (MetadataValue dcv : current) {
                if (dcv.getAuthority() == null || !csvBulkEditUtil.isAuthorityControlledField(md)) {
                    dcvalues.add(dcv.getValue());
                } else {
                    dcvalues.add(dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() +
                        csv.getAuthoritySeparator() +
                        (dcv.getConfidence() != -1 ? dcv.getConfidence() : Choices.CF_ACCEPTED));
                }
                log.debug(LogHelper.getHeader(c, "metadata_import",
                    "item_id=" + item.getID() + ",fromCSV=" + all +
                        ",found=" + dcv.getValue()));
            }
        } else {
            dcvalues = line.get(md);
        }


        // Compare from current->csv
        for (int v = 0; v < fromCSV.size(); v++) {
            String value = fromCSV.get(v);
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(c, csv, metadataField, value, fromAuthority);
            if (dcv.getAuthority() != null) {
                // Fix the CSV value to contain authority AND confidence
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv
                    .getAuthoritySeparator() + dcv.getConfidence();
                fromCSV.set(v, value);
            }

            if (StringUtils.isNotBlank(value) && !contains(value, dcvalues)) {
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
            dcv.setFromField(metadataField);
            if (value == null || !value.contains(csv.getAuthoritySeparator())) {
                simplyCopyValue(value, dcv);
            } else {
                String[] parts = value.split(csv.getAuthoritySeparator());
                dcv.setValue(parts[0]);
                dcv.setAuthority(parts[1]);
                dcv.setConfidence((parts.length > 2 ? Integer.parseInt(parts[2]) : Choices.CF_ACCEPTED));
            }

            // fromAuthority==null: with the current implementation metadata values from external authority sources
            // can only be used to add metadata, not to change or remove them
            // because e.g. an author that is not in the column "ORCID:dc.contributor.author" could still be in the
            // column "dc.contributor.author" so don't remove it
            if (StringUtils.isNotBlank(value) && !contains(value, fromCSV) && fromAuthority == null) {
                // Remove it
                log.debug(LogHelper.getHeader(c, "metadata_import",
                    "item_id=" + item.getID() + ",fromCSV=" + all +
                        ",removing_schema=" + metadataField.getSchema() +
                        ",removing_element=" + metadataField.getElement() +
                        ",removing_qualifier=" + metadataField.getQualifier() +
                        ",removing_language=" + metadataField.getLanguage()));
                changes.registerRemove(dcv);
            }
        }
    }

    /**
     * Add an item metadata with a line from CSV
     *
     * @param fromCSV The metadata from the CSV file
     * @param md      The element to compare
     * @param changes The changes object to populate
     */
    protected void add(Context c, DSpaceCSV csv, List<String> fromCSV, String md, BulkEditChange changes) {
        BulkEditMetadataField metadataField = BulkEditMetadataField.parse(md);
        AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);

        // Add all the values
        for (String value : fromCSV) {
            BulkEditMetadataValue dcv = getBulkEditValueFromCSV(c, csv, metadataField, value,
                fromAuthority);
            if (fromAuthority != null) {
                value = dcv.getValue() + csv.getAuthoritySeparator() + dcv.getAuthority() + csv
                    .getAuthoritySeparator() + dcv.getConfidence();
            }

            // Add it
            if (StringUtils.isNotBlank(value)) {
                changes.registerAdd(dcv);
            }
        }
    }

    protected BulkEditMetadataValue getBulkEditValueFromCSV(Context c, DSpaceCSV csv,
                                                            BulkEditMetadataField metadataField, String value,
                                                            AuthorityValue fromAuthority) {
        // Look to see if it should be removed
        BulkEditMetadataValue dcv = new BulkEditMetadataValue();
        dcv.setFromField(metadataField);
        if (fromAuthority != null) {
            if (value.indexOf(':') > 0) {
                value = value.substring(0, value.indexOf(':'));
            }

            // look up the value and authority in solr
            List<AuthorityValue> byValue = authorityValueService.findByValue(c, metadataField.getSchema(),
                metadataField.getElement(), metadataField.getQualifier(), value);
            AuthorityValue authorityValue = null;
            if (byValue.isEmpty()) {
                String toGenerate = fromAuthority.generateString() + value;
                String field = metadataField.getMetadataField("_");
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
            dcv.setConfidence((parts.length > 2 ? Integer.parseInt(parts[2]) : Choices.CF_ACCEPTED));
        }
        return dcv;
    }

    protected void simplyCopyValue(String value, BulkEditMetadataValue dcv) {
        dcv.setValue(value);
        dcv.setAuthority(null);
        dcv.setConfidence(Choices.CF_UNSET);
    }

    /**
     * Get metadata values from a csv line, cleaned to have authority stripped if field is not authority controlled
     * @param line DSpaceCSVLine to get values from
     * @param authoritySeparator Expected separator between value and authority
     */
    protected Map<String, List<String>> getAuthorityCleanMetadataValues(DSpaceCSVLine line, String authoritySeparator) {
        Map<String, List<String>> metadataValues = new HashMap<>();
        for (String key : line.metadataKeys()) {
            metadataValues.put(key, csvBulkEditUtil.isAuthorityControlledField(key) ?
                line.get(key) : line.get(key).stream()
                .map((value) -> StringUtils.substringBefore(value, authoritySeparator))
                .collect(Collectors.toList()));
        }
        return metadataValues;
    }

    /**
     * Method to find if a String occurs in an array of Strings
     *
     * @param needle   The String to look for
     * @param haystack The list of Strings to search through
     * @return Whether it is contained
     */
    protected boolean contains(String needle, List<String> haystack) {
        // Look for the needle in the haystack
        return haystack.stream().anyMatch((examine) -> StringUtils.equals(clean(examine), clean(needle)));
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
}
