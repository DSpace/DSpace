/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.lang3.Strings;
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
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
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
public class CSVBulkEditParsingServiceImpl extends AbstractBulkEditParsingService<DSpaceCSV, DSpaceCSVLine> {
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
     * List of errors detected during relation validation
     */
    protected ArrayList<String> relationValidationErrors = new ArrayList<>();

    @Override
    protected void cleanUp(Context context) throws SQLException, MetadataImportException {
        validateExpressedRelations(context, source);
    }

    @Override
    protected DSpaceCSV read(Context c, InputStream is) throws IOException, SQLException, MetadataImportException {
        return new DSpaceCSV(is, c);
    }

    @Override
    protected List<DSpaceCSVLine> getRecords() throws SQLException, MetadataImportException {
        return source.getCSVLines();
    }

    @Override
    protected BulkEditChange initChange(Context c, DSpaceCSVLine line) throws SQLException, MetadataImportException {
        BulkEditChange whatHasChanged;

        if (line.getID() != null) {
            whatHasChanged = new BulkEditChange(itemService.find(c, line.getID()));
        } else {
            whatHasChanged = new BulkEditChange(new UUID(0, rowCount));
        }

        return whatHasChanged;
    }

    @Override
    protected List<BulkEditMetadataValue> parseMetadata(Context c, DSpaceCSVLine line, BulkEditChange whatHasChanged) {
        List<BulkEditMetadataValue> pending = new ArrayList<>();
        Map<String, List<String>> cleanByField = getAuthorityCleanMetadataValues(line, source.getAuthoritySeparator());

        for (String md : cleanByField.keySet()) {
            BulkEditMetadataField metadataField = BulkEditMetadataField.parse(md);
            AuthorityValue fromAuthority = authorityValueService.getAuthorityValueType(md);

            List<String> fromCSV = cleanByField.get(md);
            if (fromCSV.isEmpty()) {
                whatHasChanged.registerClear(BulkEditMetadataField.parse(md));
                continue;
            }

            for (String value : fromCSV) {
                BulkEditMetadataValue dcv = getBulkEditValueFromCSV(c, source, metadataField, value, fromAuthority);
                if (fromAuthority != null) {
                    value = StringUtils.joinWith(
                        source.getAuthoritySeparator(), dcv.getValue(), dcv.getAuthority(), dcv.getConfidence()
                    );
                }

                if (StringUtils.isNotBlank(value)) {
                    pending.add(dcv);
                }
            }
        }

        return pending;
    }

    @Override
    protected void resolveCollections(
        Context c, DSpaceCSVLine line, BulkEditChange whatHasChanged
    ) throws MetadataImportException, SQLException {
        // Check it has an owning collection
        List<String> collections = line.get("collection");
        if (collections == null) {
            collections = List.of();
        }

        // Record the addition to collections
        List<Collection> foundCollections = new ArrayList<>();
        for (String handle : collections) {
            // Resolve the handle to the collection
            DSpaceObject collection = handleService.resolveToObject(c, handle);

            // Check it resolved OK
            if (!(collection instanceof Collection)) {
                throw new MetadataImportException(
                    "'" + handle + "' is not a Collection! You must specify a valid collection for new items");
            }

            // Check for duplicate
            if (foundCollections.contains(collection)) {
                throw new MetadataImportException("Duplicate collection assignment detected in new item: " + handle);
            } else {
                foundCollections.add((Collection) collection);
            }
        }

        whatHasChanged.registerCollections(foundCollections);
    }

    @Override
    protected void additionalParsingSteps(Context c, DSpaceCSVLine line, BulkEditChange whatHasChanged) {
        setIdentifiers(line, whatHasChanged);
        populateReferenceMaps(line, rowCount, whatHasChanged.getUuid());
    }

    @Override
    protected void resolveAction(
        Context c, DSpaceCSVLine line, BulkEditChange whatHasChanged
    ) throws MetadataImportException {
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
     * Gets a copy of the given csv line with all entity target references resolved to UUID strings.
     * Keys being iterated over represent metadatafields or special columns to be processed.
     *
     * @param line the csv line to process.
     * @return a copy, with all references resolved.
     * @throws MetadataImportException if there is an error resolving any entity target reference.
     */
    @Override
    protected DSpaceCSVLine preprocessRecord(Context c, DSpaceCSVLine line) throws MetadataImportException {
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

        boolean isAuthorityControlled = metadataAuthorityService.isAuthorityAllowed(
            metadataField.getMetadataField("_"), Constants.ITEM, null);

        if (fromAuthority != null && isAuthorityControlled) {
            if (value.indexOf(':') > 0) {
                value = value.substring(0, value.indexOf(':'));
            }

            // look up the value and authority in solr
            List<AuthorityValue> byValue = authorityValueService.findByValue(metadataField.getSchema(),
                metadataField.getElement(), metadataField.getQualifier(), value);
            AuthorityValue authorityValue = null;
            if (byValue.isEmpty()) {
                String toGenerate = fromAuthority.generateString() + value;
                String field = metadataField.getMetadataField("_");
                authorityValue = authorityValueService.generate(toGenerate, value, field);
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
        return haystack.stream().anyMatch((examine) -> Strings.CS.equals(clean(examine), clean(needle)));
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
            String[] mf = reference.substring(0, i).split("\\.");
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
                    // Add to errors if Relationship.type cannot be derived
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
                            // Add to errors if Relationship.type cannot be derived.
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
