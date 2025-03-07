package org.dspace.app.bulkedit.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditCacheServiceImpl implements BulkEditCacheService {
    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected EntityTypeService entityTypeService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    /**
     * Map of field:value to csv row number, used to resolve indirect entity target references.
     */
    protected Map<String, Set<Integer>> csvRefMap = new HashMap<>();

    /**
     * Map of csv row number to UUID, used to resolve indirect entity target references.
     */
    protected HashMap<Integer, UUID> csvRowMap = new HashMap<>();

    /**
     * Map of UUIDs to their entity types.
     */
    protected HashMap<UUID, String> entityTypeMap = new HashMap<>();

    /**
     * Map of UUIDs to their relations that are referenced within any import with their referrers.
     */
    protected HashMap<String, HashMap<String, ArrayList<String>>> entityRelationMap = new HashMap<>();

    protected ArrayList<String> relationValidationErrors = new ArrayList<>();

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
     * @param values the values ordered by field.
     * @param uuid the uuid of the item, which may be null if it has not been minted yet.
     */
    public void populateRefAndRowMap(Map<String, List<String>> values, Integer row, @Nullable UUID uuid) {
        if (uuid != null) {
            csvRowMap.put(row, uuid);
        } else {
            csvRowMap.put(row, new UUID(0, row));
        }
        for (String key : values.keySet()) {
            if (key.contains(".") && !key.split("\\.")[0].equalsIgnoreCase("relation") ||
                key.equalsIgnoreCase("rowName")) {
                for (String value : values.get(key)) {
                    String valueKey = key + ":" + value;
                    Set<Integer> rowNums = csvRefMap.get(valueKey);
                    if (rowNums == null) {
                        rowNums = new HashSet<>();
                        csvRefMap.put(valueKey, rowNums);
                    }
                    rowNums.add(row);
                }
            }
            //Populate entityTypeMap
            if (key.equalsIgnoreCase("dspace.entity.type") && !values.get(key).isEmpty()) {
                if (uuid == null) {
                    entityTypeMap.put(new UUID(0, row), values.get(key).get(0));
                } else {
                    entityTypeMap.put(uuid, values.get(key).get(0));
                }
            }
        }
    }

    /**
     * Gets the set of matching lines as UUIDs that have already been processed given a metadata value.
     *
     * @param mdValueRef the metadataValue reference to search for.
     * @return the set of matching lines as UUIDs.
     */
    public Set<UUID> getMatchingCSVUUIDs(String mdValueRef) {
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
    public UUID getUUIDForRow(int rowNum) {
        if (csvRowMap.containsKey(rowNum)) {
            return csvRowMap.get(rowNum);
        } else {
            return new UUID(0, rowNum);
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
     * The reference may refer to a previously-processed item in the CSV or an item in the database.
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
        // Lookup UUIDs that may have already been processed into the csvRefMap
        // See populateRefAndRowMap() for how the csvRefMap is populated
        // See getMatchingCSVUUIDs() for how the reference param is sourced from the csvRefMap
        Set<UUID> csvUUIDs = getMatchingCSVUUIDs(reference);
        if (csvUUIDs.size() > 1) {
            throw new MetadataImportException("Error resolving Entity reference:\n" +
                "Ambiguous reference; multiple matches in csv: " + reference);
        } else if (csvUUIDs.size() == 1) {
            UUID csvUUID = csvUUIDs.iterator().next();
            if (csvUUID.equals(uuid)) {
                return uuid; // one match from csv and db (same item)
            } else if (uuid != null) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
                    "Ambiguous reference; multiple matches in db and csv: " + reference);
            } else {
                return csvUUID; // one match from csv
            }
        } else { // size == 0; the reference does not exist throw an error
            if (uuid == null) {
                throw new MetadataImportException("Error resolving Entity reference:\n" +
                    "No matches found for reference: " + reference
                    + "\nKeep in mind you can only reference entries that are " +
                    "listed before " +
                    "this one within the CSV.");
            } else {
                return uuid; // one match from db
            }
        }
    }
}
