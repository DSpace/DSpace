package org.dspace.app.bulkedit.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.core.Context;

public interface BulkEditCacheService {
    void resetCache();
    void populateMetadataReferenceMap(Map<String, List<String>> values, UUID uuid);
    void populateEntityRelationMap(String refUUID, String relationField, String originId);
    UUID resolveEntityRef(Context context, String reference) throws MetadataImportException;
}
