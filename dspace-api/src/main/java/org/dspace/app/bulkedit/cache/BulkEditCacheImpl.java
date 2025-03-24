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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;

public class BulkEditCacheImpl implements BulkEditCache {
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

    @Override
    public void resetCache() {
        metadataReferenceToUUIDMap = new HashMap<>();
        entityTypeMap = new HashMap<>();
        entityRelationMap = new HashMap<>();
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
}
