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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.BulkEditMetadataField;
import org.dspace.app.bulkedit.BulkEditMetadataValue;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.util.RelationshipUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the service for parsing bulk-edit changes from a {@link DSpaceCSV} to a list of
 * {@link BulkEditChange}s
 *
 * Warning: This service is stateful, in that a new instance will be created every time it is requested.
 *          This is by design because the service will keep information about multiple related changes until
 *          it is done parsing them all and this ensures none of the information leaks between other calls/processes.
 *          This means the service should never be Autowired and should instead be requested through the
 *          {@link BulkEditServiceFactory} wherever the call is made to parse and/or apply the changes.
 */
public class CSVBulkEditParsingServiceImpl implements BulkEditParsingService<DSpaceCSV> {
    protected static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(CSVBulkEditParsingServiceImpl.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected HandleService handleService;

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected AuthorityValueService authorityValueService;

    @Autowired
    protected EntityTypeService entityTypeService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;

    /**
     * Map of field:value to item UUID, used to resolve indirect entity target references.
     */
    protected Map<String, Set<UUID>> metadataReferenceToUUIDMap = new HashMap<>();

    /**
     * Map of UUIDs to their entity types.
     */
    protected Map<UUID, String> entityTypeMap = new HashMap<>();

    /**
     * Map of UUIDs to their relations that are referenced within any import with their referrers.
     */
    protected Map<String, HashMap<String, ArrayList<String>>> entityRelationMap = new HashMap<>();

    /**
     * Map with item UUIDs for each CSV row
     */
    protected Map<Integer, UUID> csvRowMap = new HashMap<>();

    /**
     * Counter of rows processed in a CSV.
     */
    protected Integer rowCount = 1;

    /**
     * List of errors detected during relation validation
     */
    protected ArrayList<String> relationValidationErrors = new ArrayList<>();

    /**
     * DSpaceRunnableHandler to log register changes
     */
    protected DSpaceRunnableHandler handler;

    public List<BulkEditChange> parse(Context c, DSpaceCSV csv)
        throws MetadataImportException, SQLException, AuthorizeException, IOException {
        Context.Mode lastMode = c.getCurrentMode();

        try {
            // Force a READ ONLY mode to make it clear no actual changes are meant to be made during a register
            c.setMode(Context.Mode.READ_ONLY);

            List<BulkEditChange> changes = new ArrayList<>();

            for (DSpaceCSVLine line : csv.getCSVLines()) {
                parseLine(c, csv, line, changes);
            }

            validateExpressedRelations(c, csv);

            return changes;
        } finally {
            // Restore the Context Mode
            c.setMode(lastMode);
        }
    }

    /**
     * Parse a single {@link DSpaceCSVLine} and add its {@link BulkEditChange} result to the list of changes
     * @param c         DSpace Context
     * @param csv       Source DSpaceCSV
     * @param line      DSpaceCSVLine to create a BulkEditChange for
     * @param changes   List of changes to add the created change to
     */
    protected void parseLine(Context c, DSpaceCSV csv, DSpaceCSVLine line, List<BulkEditChange> changes)
        throws MetadataImportException, SQLException, AuthorizeException, IOException {
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

        BulkEditChange whatHasChanged = id != null ?
            parseEditChange(c, csv, line, id, metadataValues) :
            parseAddChange(c, csv, line, metadataValues);
        if (whatHasChanged != null) {
            finalizeParseLine(c, csv, line, whatHasChanged);
            changes.add(whatHasChanged);
        }

        rowCount++;
    }

    /**
     * Parse the change of an item from a {@link DSpaceCSVLine} and return its created {@link BulkEditChange}
     * @param c         DSpace Context
     * @param csv       Source DSpaceCSV
     * @param line      DSpaceCSVLine to create a BulkEditChange for
     * @param id        UUID of an existing item
     * @param metadataValues Map of raw metadata values from the csv line where the key is the metadata field
     */
    protected @Nullable BulkEditChange parseEditChange(Context c, DSpaceCSV csv,
                                                       DSpaceCSVLine line, UUID id,
                                                       Map<String, List<String>> metadataValues)
        throws MetadataImportException, SQLException, AuthorizeException, IOException {
        // Get the item
        Item item = itemService.find(c, id);
        if (item == null) {
            throw new MetadataImportException("Unknown item ID " + id);
        }

        // Record changes
        BulkEditChange whatHasChanged = new BulkEditChange(item);

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

        parseAction(line, whatHasChanged);

        // Only record if changes have been made
        if (whatHasChanged.hasChanges()) {
            return whatHasChanged;
        }

        return null;
    }

    /**
     * Parse the addition of an item from a {@link DSpaceCSVLine} and return its created {@link BulkEditChange}
     * @param c         DSpace Context
     * @param csv       Source DSpaceCSV
     * @param line      DSpaceCSVLine to create a BulkEditChange for
     * @param metadataValues Map of raw metadata values from the csv line where the key is the metadata field
     */
    protected @Nullable BulkEditChange parseAddChange(Context c, DSpaceCSV csv, DSpaceCSVLine line,
                                                      Map<String, List<String>> metadataValues)
        throws MetadataImportException, SQLException, AuthorizeException, IOException {
        // First check a user is set, otherwise this can't happen
        if (c.getCurrentUser() == null) {
            throw new MetadataImportException(
                "When adding new items, a user must be specified with the -e option");
        }

        // Iterate through each metadata element in the csv line
        BulkEditChange whatHasChanged = new BulkEditChange(new UUID(0, rowCount));
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
        return whatHasChanged;
    }

    /**
     * Finalize the parsing of a {@link DSpaceCSVLine} into a {@link BulkEditChange}
     * Final touches include storing optional identifiers, logging the register to the handler and populating reference
     * maps in our cache for following line registers
     * @param c         DSpace Context
     * @param csv       Source DSpaceCSV
     * @param line      DSpaceCSVLine to create a BulkEditChange for
     * @param whatHasChanged The created BulkEditChange to perform final touches for, before adding it to the list
     */
    protected void finalizeParseLine(Context c, DSpaceCSV csv, DSpaceCSVLine line, BulkEditChange whatHasChanged) {
        setIdentifiers(line, whatHasChanged);

        if (handler != null) {
            logParse(csv, whatHasChanged);
        }
        populateReferenceMaps(line, rowCount, whatHasChanged.getUuid());
    }

    /**
     * Parse the line's action
     * @param line              DSpaceCSVLine to read action from
     * @param whatHasChanged    BulkEditChange to register action into
     */
    protected void parseAction(DSpaceCSVLine line, BulkEditChange whatHasChanged) throws MetadataImportException {
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
                if (!whatHasChanged.getItem().isWithdrawn()) {
                    whatHasChanged.setWithdrawn();
                }
            } else if ("reinstate".equals(action)) {
                // Reinstate the item
                if (whatHasChanged.getItem().isWithdrawn()) {
                    whatHasChanged.setReinstated();
                }
            } else {
                // Unknown action!
                throw new MetadataImportException("Unknown action: " + action);
            }
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
        UUID originId = evaluateOriginId(line.getID());
        for (String key : line.keys()) {
            // If a key represents a relation field attempt to resolve the target reference from the csvRefMap
            if (key.split("\\.")[0].equalsIgnoreCase("relation")) {
                if (!line.get(key).isEmpty()) {
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
                metadataField.getElement(), metadataField.getQualifier(), metadataField.getLanguage(),
                StringUtils.equals(metadataField.getSchema(), MetadataSchemaEnum.RELATION.getName()));
            for (MetadataValue dcv : current) {
                if (dcv.getAuthority() == null || !isAuthorityControlledField(md)) {
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
                resolveValueAndAuthority(csv, value, dcv);
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

    /**
     * Store optional identifiers from the CSV line into the BulkEditChange
     * @param line      DSpaceCSVLine to retrieve optional identifiers from
     * @param bechange  BulkEditChange to store the optional identifiers into
     */
    protected void setIdentifiers(DSpaceCSVLine line, BulkEditChange bechange) {
        List<String> rowName = line.get("rowName");
        if (CollectionUtils.isNotEmpty(rowName)) {
            bechange.setIdentifier("rowName", rowName.get(0));
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
            resolveValueAndAuthority(csv, value, dcv);
        }
        return dcv;
    }

    protected void simplyCopyValue(String value, BulkEditMetadataValue dcv) {
        dcv.setValue(value);
        dcv.setAuthority(null);
        dcv.setConfidence(Choices.CF_UNSET);
    }

    protected void resolveValueAndAuthority(DSpaceCSV csv, String value, BulkEditMetadataValue dcv) {
        // Cells with valid authority are composed of three parts ~ <value>, <authority>, <confidence>
        // The value itself may also include the authority separator though
        String[] parts = value.split(csv.getEscapedAuthoritySeparator());

        // If we don't have enough parts, assume the whole string is the value
        if (parts.length < 3) {
            simplyCopyValue(value, dcv);
            return;
        }

        try {
            // The last part of the cell must be a confidence value (integer)
            int confidence = Integer.parseInt(parts[parts.length - 1]);
            String authority = parts[parts.length - 2];
            String plainValue = String.join(
                csv.getAuthoritySeparator(),
                ArrayUtils.subarray(parts, 0, parts.length - 2)
            );

            dcv.setValue(plainValue);
            dcv.setAuthority(authority);
            dcv.setConfidence(confidence);
        } catch (NumberFormatException e) {
            // Otherwise assume the whole string is the value
            simplyCopyValue(value, dcv);
        }
    }

    /**
     * Get metadata values from a csv line, cleaned to have authority stripped if field is not authority controlled
     * @param line DSpaceCSVLine to get values from
     * @param authoritySeparator Expected separator between value and authority
     */
    protected Map<String, List<String>> getAuthorityCleanMetadataValues(DSpaceCSVLine line, String authoritySeparator) {
        Map<String, List<String>> metadataValues = new TreeMap<>();
        for (String key : line.metadataKeys()) {
            metadataValues.put(key, isAuthorityControlledField(key) ?
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

    /**
     * Log the parsing of an item change to the handler
     * @param csv       CSV origin
     * @param change    BulkEditChange of the item
     */
    protected void logParse(DSpaceCSV csv, BulkEditChange change) {
        boolean isAdd = change.isNewItem();

        List<String> info = new ArrayList<>();

        if (change.getItem() != null) {
            info.add("uuid=" + change.getItem().getID());
        }

        handler.logInfo(String.format(
            "Row %d/%d: Parsed Item %s %s",
            rowCount, csv.getCSVLines().size(),
            isAdd ? "import" : "update",
            info.isEmpty() ? "" : "(" + StringUtils.join(info, ", ") + ")"
        ));
    }

    @Override
    public void setHandler(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

    /**
     * Populates the metadataReferenceToUUIDMap, and entityTypeMap for the given metadata values.
     *
     * The metadataReferenceToUUIDMap is an index that keeps track of which bulk edit items have a specific value for
     * a specific metadata field or the special "rowName" column. This is used to help resolve indirect
     * entity target references in the same batch edit.
     *
     * @param values the values ordered by column header (metadata fields and other).
     * @param uuid the uuid of the item, which may be a placeholder one in case it's not minted yet.
     */
    public void populateMetadataReferenceMap(Map<String, List<String>> values, UUID uuid) {
        for (String key : values.keySet()) {
            if ((key.contains(".") && !key.split("\\.")[0].equalsIgnoreCase("relation")) ||
                key.equalsIgnoreCase("rowName")) {
                for (String value : values.get(key)) {
                    String valueKey = key + ":" + value;
                    if (!metadataReferenceToUUIDMap.containsKey(valueKey)) {
                        metadataReferenceToUUIDMap.put(valueKey, new HashSet<>());
                    }
                    metadataReferenceToUUIDMap.get(valueKey).add(uuid);
                }
            }
            //Populate entityTypeMap
            if (key.equalsIgnoreCase("dspace.entity.type") && !values.get(key).isEmpty()) {
                entityTypeMap.put(uuid, values.get(key).get(0));
            }
        }
    }

    /**
     * Populate the entityRelationMap with all target references and it's associated typeNames
     * to their respective origins
     *
     * @param refUUID the target reference UUID for the relation
     * @param relationField the field of the typeNames to relate from
     */
    public void populateEntityRelationMap(String refUUID, String relationField, String originId) {
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
     * Gets the UUID of the item indicated by the given target reference,
     * which may be a direct UUID string, a row reference
     * of the form rowName:VALUE, or a metadata value reference of the form schema.element[.qualifier]:VALUE.
     *
     * The reference may refer to a previously-processed item in the batch edit or an item in the database.
     *
     * @param context the context to use.
     * @param reference the target reference which may be a UUID, metadata reference, or rowName reference.
     * @return the uuid.
     * @throws MetadataImportException if the target reference is malformed or ambiguous (refers to multiple items).
     */
    public UUID resolveEntityRef(Context context, String reference) throws MetadataImportException {
        // value reference
        UUID uuid = null;
        if (!reference.contains(":")) {
            // assume it's a UUID
            try {
                return UUID.fromString(reference);
            } catch (IllegalArgumentException e) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
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
                throw new MetadataImportException("Error resolving Entity reference:\n" +
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
                        throw new MetadataImportException("Error resolving Entity reference:\n" +
                            "Ambiguous reference; multiple matches in db: " + reference);
                    }
                }
            } catch (SQLException e) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
                    "Error looking up item by metadata reference: " + reference, e);
            }
        }
        // Lookup UUIDs that may have already been processed
        // See populateRefAndRowMap() for how the Map is populated
        Set<UUID> referencedUUIDs = metadataReferenceToUUIDMap.get(reference);
        if (CollectionUtils.isEmpty(referencedUUIDs)) {
            // size == 0; the reference does not exist throw an error
            if (uuid == null) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
                    "No matches found for reference: " + reference
                    + "\nKeep in mind you can only reference entries that are " +
                    "listed before " +
                    "this one within the batch edit.");
            } else {
                return uuid; // one match from db
            }
        } else if (referencedUUIDs.size() > 1) {
            throw new MetadataImportException("Error resolving Entity reference:\n" +
                "Ambiguous reference; multiple matches in import: " + reference);
        } else {
            UUID batchEditUUID = referencedUUIDs.iterator().next();
            if (batchEditUUID.equals(uuid)) {
                return uuid; // one match from batch edit and db (same item)
            } else if (uuid != null) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
                    "Ambiguous reference; multiple matches in db and import: " + reference);
            } else {
                return batchEditUUID; // one match from batch edit
            }
        }
    }

    public void populateReferenceMaps(DSpaceCSVLine line, Integer rowNumber, UUID uuid) {
        Map<String, List<String>> valueMap = new HashMap<>();
        for (String key : line.keys()) {
            valueMap.put(key, line.get(key));
        }
        populateMetadataReferenceMap(valueMap, uuid);
        csvRowMap.put(rowNumber, uuid);
    }

    /**
     * Return a UUID of the origin in process or a placeholder for the origin to be evaluated later
     *
     * @param originId UUID of the origin
     * @return the UUID of the item or UUID placeholder
     */
    public UUID evaluateOriginId(@Nullable UUID originId) {
        if (originId != null) {
            return originId;
        } else {
            return new UUID(0, rowCount);
        }
    }

    public void validateExpressedRelations(Context c, DSpaceCSV csv) throws MetadataImportException {
        relationValidationErrors = new ArrayList<>();

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
                                DSpaceCSVLine dSpaceCSVLine = csv.getCSVLines()
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

        }

        // If relationValidationErrors is empty all described relationships are valid.
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

    public boolean isAuthorityControlledField(String field, String separator) {
        return metadataAuthorityService.isAuthorityControlled(getCleanMdField(field, separator, "_"));
    }

    public boolean isAuthorityControlledField(String field) {
        return isAuthorityControlledField(field, ".");
    }

    public String getCleanMdField(String field, String originalSeparator, String newSeparator) {
        String mdf = field;
        if (StringUtils.contains(mdf, ":")) {
            mdf = StringUtils.substringAfter(field, ":");
        }
        if (StringUtils.contains(mdf, "[")) {
            mdf = StringUtils.substringBefore(mdf, "[");
        }
        if (!StringUtils.contains(mdf, newSeparator)) {
            mdf = mdf.replaceAll(Pattern.quote(originalSeparator), newSeparator);
        }
        return mdf;
    }
}
