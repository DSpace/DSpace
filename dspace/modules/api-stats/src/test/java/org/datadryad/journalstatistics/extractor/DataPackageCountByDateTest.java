/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.test.ContextUnitTest;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageCountByDateTest extends ContextUnitTest{
    private Date futureDate;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 9999);
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        futureDate = calendar.getTime();
    }

    @Test
    public void testCountDataPackagesByDate() throws SQLException {
        // Count the initial number of data packages
        String journalName = "Test Journal";
        DataPackageCount instance = new DataPackageCount(this.context);
        instance.setEndDate(futureDate);
        Integer initialCount = instance.extract(journalName);
        // Create a new data package. It will have today's date
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(journalName);
        Integer expResult = initialCount + 1;
        Integer result = instance.extract(journalName);
        assertEquals(expResult, result);

        // update the date accessioned of the data package, and assert the count
        // goes down
        fail("write this part of the test");

    }
}
