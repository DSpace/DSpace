/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadObject;
import org.dspace.core.Context;

/**
 * Counts the total size (in bytes) of Dryad Data Files in the archive
 * associated with a specified Journal
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileTotalSize extends DataFileCount {
    private static Logger log = Logger.getLogger(DataFileTotalSize.class);
    public DataFileTotalSize(Context context) {
        super(context);
    }

    @Override
    Long countValue(DryadObject dryadObject) {
        DryadDataFile dataFile = (DryadDataFile)dryadObject;
        Long totalSize = 0l;
        try {
            totalSize = dataFile.getTotalStorageSize();
        } catch (SQLException ex) {
            log.error("Error getting total size of data file", ex);
        }
        return totalSize;
    }
}
