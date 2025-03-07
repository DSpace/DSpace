package org.dspace.app.bulkedit.service;

import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.core.Context;

public interface CSVBulkEditCacheService extends BulkEditCacheService {
    void populateRefAndRowMap(DSpaceCSVLine line, @Nullable UUID uuid);
    Set<String> getAuthorityControlledFields();
    boolean isAuthorityControlledField(String field);
    Integer getRowCount();
    void setRowCount(Integer rowCount);
    void increaseRowCount();
    void resetRowCount();
    UUID evaluateOriginId(@Nullable UUID originId);
    void validateExpressedRelations(Context c, DSpaceCSV csv) throws MetadataImportException;
}
