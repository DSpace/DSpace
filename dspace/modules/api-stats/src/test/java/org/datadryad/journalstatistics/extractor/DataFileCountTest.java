/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.test.ContextUnitTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileCountTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(DataFileCountTest.class);

    /**
     * Test of extract method, of class DataFileCount.
     * Data files are items in collection identified by 'stats.datafiles.coll'
     * belonging to a package with prism.publicationName as provided
     */
    @Test
    public void testCountDataFiles() throws SQLException {
        log.info("countDataFiles");
        // Count the initial number of data files
        String journalName = "Test Journal";
        DataFileCount instance = new DataFileCount(this.context);
        Long initialCount = instance.extract(journalName);
        // Create a new data package, and assert the count goes up by one
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        DryadDataFile dataFile = DryadDataFile.create(context, dataPackage);
        dataPackage.setPublicationName(journalName);
        Long expResult = initialCount + 1;
        Long result = instance.extract(journalName);
        assertEquals("Data file count should increase by 1", expResult, result);
    }
}
