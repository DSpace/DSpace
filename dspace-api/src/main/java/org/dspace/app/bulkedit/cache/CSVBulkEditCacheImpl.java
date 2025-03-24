/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.cache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.util.RelationshipUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;

public class CSVBulkEditCacheImpl extends BulkEditCacheImpl implements CSVBulkEditCache {
    protected ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();
    protected EntityTypeService entityTypeService =
        ContentServiceFactory.getInstance().getEntityTypeService();
    protected RelationshipTypeService relationshipTypeService =
        ContentServiceFactory.getInstance().getRelationshipTypeService();

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

    @Override
    public void resetCache() {
        super.resetCache();
        csvRowMap = new HashMap<>();
        relationValidationErrors = new ArrayList<>();
        rowCount = 1;
    }

    @Override
    public void populateReferenceMaps(DSpaceCSVLine line, Integer rowNumber, UUID uuid) {
        Map<String, List<String>> valueMap = new HashMap<>();
        for (String key : line.keys()) {
            valueMap.put(key, line.get(key));
        }
        populateMetadataReferenceMap(valueMap, uuid);
        csvRowMap.put(rowNumber, uuid);
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public void increaseRowCount() {
        rowCount++;
    }

    public void resetRowCount() {
        rowCount = 1;
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

    @Override
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
}
