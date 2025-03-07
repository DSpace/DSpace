package org.dspace.app.bulkedit.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.core.Context;

public interface BulkEditCacheService {
    void populateRefAndRowMap(Map<String, List<String>> values, Integer row, @Nullable UUID uuid);
    void populateEntityRelationMap(String refUUID, String relationField, String originId);
    Set<UUID> getMatchingCSVUUIDs(String mdValueRef);
    UUID getUUIDForRow(int rowNum);
    UUID resolveEntityRef(Context context, String reference) throws MetadataImportException;
}
