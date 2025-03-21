package org.dspace.app.bulkedit.cache;

import java.util.UUID;
import javax.annotation.Nullable;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.core.Context;

public interface CSVBulkEditCache extends BulkEditCache {
    void populateReferenceMaps(DSpaceCSVLine line, Integer rowNumber, UUID uuid);
    Integer getRowCount();
    void setRowCount(Integer rowCount);
    void increaseRowCount();
    void resetRowCount();
    UUID evaluateOriginId(@Nullable UUID originId);
    void validateExpressedRelations(Context c, DSpaceCSV csv) throws MetadataImportException;
}
