/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.datadryad.api.DryadDataFile;
import org.datadryad.test.ContextUnitTest;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class EmbargoedDataFileCountTest extends ContextUnitTest {

    private Date futureDate;

    @Before
    public void setUp() {
        super.setUp();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 9999);
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        futureDate = calendar.getTime();
    }

    /**
     * Test of extract method, of class DataPackageCount.
     * Data packages are items in collection identified by 'stats.datapkgs.coll'
     * having prism.publicationName as provided
     */
    @Test
    public void testCountEmbargoedDataFiles() throws SQLException {
        // Count the initial number of embargoed data files
        String journalName = "Test Journal";
        EmbargoedDataFileCount instance = new EmbargoedDataFileCount(this.context);
        Integer initialCount = instance.extract(journalName);
        // Create a new data package, do not set an embargo, assert the count does not change
        DryadDataFile dataFile = DryadDataFile.create(context);
        dataFile.setPublicationName(journalName);
        Integer expResult = initialCount;
        Integer result = instance.extract(journalName);
        assertEquals(expResult, result);

        // embargo the file and assert the count changes
        dataFile.setEmbargo("custom", futureDate );
        result = instance.extract(journalName);
        expResult = initialCount + 1;
        assertEquals(expResult, result);

        // Clear the embargo and assert the count goes back down
        dataFile.clearEmbargo();
        result = instance.extract(journalName);
        expResult = initialCount;
        assertEquals(expResult, result);
    }
}
