/*
 */
package org.datadryad.journalstatistics.extractor;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadObject;
import org.dspace.core.Context;

/**
 * Counts Dryad Data Files in the archive associated with a specified Journal
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class EmbargoedDataFileCount extends DataFileCount {
    private static Logger log = Logger.getLogger(DataItemCount.class);
    public EmbargoedDataFileCount(Context context) {
        super(context);
    }

    // Count the things under embargo
    // This is accomplished by metadata
    @Override
    Boolean filter(DryadObject dryadObject) {
        DryadDataFile dataFile = (DryadDataFile)dryadObject;
        return dataFile.isEmbargoed();
    }
}
