/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileCountTest {

    private Context context;

    @Before
    public void setUp() {
        try {
            this.context = new Context();
            context.turnOffAuthorisationSystem();
        } catch (SQLException ex) {
            fail("Unable to instantiate Context " + ex);
        }
    }

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
        assertEquals(expResult, result);
    }
}
