/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.datadryad.api.DryadDataFile;
import org.datadryad.test.ContextUnitTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileCountTest extends ContextUnitTest {

    /**
     * Test of extract method, of class DataFileCount.
     * Data files are items in collection identified by 'stats.datafiles.coll'
     * having prism.publicationName as provided
     */
    @Test
    public void testCountDataFiles() throws SQLException {
        // Count the initial number of data files
        String journalName = "Test Journal";
        DataFileCount instance = new DataFileCount(this.context);
        Integer initialCount = instance.extract(journalName);
        // Create a new data package, and assert the count goes up by one
        DryadDataFile dataFile = DryadDataFile.create(context);
        dataFile.setPublicationName(journalName);
        Integer expResult = initialCount + 1;
        Integer result = instance.extract(journalName);
        assertEquals("Data file count should increase by 1", expResult, result);
    }
}
