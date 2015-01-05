/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
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
    private static Logger log = Logger.getLogger(DataPackageCountByDateTest.class);
    private Date futureDate;
    private Date lastYearDate;
    private Date customDate_2008_07_25;
    private Date customDate_2012_03_21;
    private Date customDate_2010_01_01;
    private Date customDate_2010_01_02;
    private final static String journalName = "Test Journal";
    @Before
    @Override
    public void setUp() {
        super.setUp();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 9999);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        futureDate = calendar.getTime();
        calendar = new GregorianCalendar();
        calendar.add(Calendar.YEAR, -1);
        lastYearDate = calendar.getTime();
        calendar.set(Calendar.YEAR, 2008);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 25);
        customDate_2008_07_25 = calendar.getTime();
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, 21);
        customDate_2012_03_21 = calendar.getTime();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        customDate_2010_01_01 = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        customDate_2010_01_02 = calendar.getTime();
    }

    /**
     * Test that a date range count ending in the distant future includes
     * a package deposited today.
     * @throws SQLException
     */
    @Test
    public void testCountDataPackagesByDate() throws SQLException {
        log.info("countDataPackagesByDate");
        // Count the initial number of data packages
        DataPackageCount instance = new DataPackageCount(this.context);
        instance.setEndDate(futureDate);
        // Assert we're filtering on dates
        assert(instance.filterOnDates);
        Long initialCount = instance.extract(journalName);

        // Create a new data package. It will have today's date as accessioned
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(journalName);
        Long expResult = initialCount + 1;
        Long result = instance.extract(journalName);
        assertEquals(expResult, result);
    }

    /**
     * Test that a date-range count ending last year does not include
     * a package added today.
     * @throws SQLException
     */
    @Test
    public void testCountDataPackagesByDatePast() throws SQLException {
        log.info("countDataPackagesByDatePast");
        DataPackageCount instance = new DataPackageCount(this.context);
        instance.setEndDate(lastYearDate);
        // Assert we're filtering on dates
        assert(instance.filterOnDates);
        Long initialCount = instance.extract(journalName);

        // Create a new data package. It will have today's date as accessioned
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(journalName);
        Long expResult = initialCount;
        Long result = instance.extract(journalName);
        assertEquals(expResult, result);
    }

    @Test(expected=IllegalStateException.class)
    public void testThrowsExceptionWithBadDateOrder() throws SQLException {
        log.info("throwsExceptionWithBadDateOrder");
        DataPackageCount instance = new DataPackageCount(this.context);
        assert(futureDate.after(lastYearDate));
        // Create a new data package so that there is something to extract
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(journalName);
        instance.setBeginDate(futureDate);
        instance.setEndDate(lastYearDate);
        instance.extract(journalName);
    }

    /**
     * Test that a custom date range is working
     * @throws SQLException
     */
    @Test
    public void testCountDataPackagesByDateCustom() throws SQLException {
        log.info("countDataPackagesByDateCustom");
        DataPackageCount instance = new DataPackageCount(this.context);
        instance.setBeginDate(customDate_2008_07_25);
        instance.setEndDate(customDate_2012_03_21);
        Long initialCount = instance.extract(journalName);
        
        DryadDataPackage dataPackage = DryadDataPackage.create(this.context);
        dataPackage.setPublicationName(journalName);
        dataPackage.setDateAccessioned(customDate_2010_01_01);

        Long expResult = initialCount + 1;
        Long result = instance.extract(journalName);
        assertEquals(expResult, result);
    }

    /**
     * Test that the endpoints are included in the date range
     * @throws SQLException
     */
    @Test
    public void testCountDataPackagesFencePost() throws SQLException {
        log.info("countDataPackagesFencePost");
        DataPackageCount instance = new DataPackageCount(this.context);
        instance.setBeginDate(customDate_2008_07_25);
        instance.setEndDate(customDate_2010_01_01);
        Long initialCount = instance.extract(journalName);

        DryadDataPackage dataPackage = DryadDataPackage.create(this.context);
        dataPackage.setPublicationName(journalName);
        dataPackage.setDateAccessioned(customDate_2010_01_01);

        Long expResult = initialCount + 1;
        Long result = instance.extract(journalName);
        assertEquals(expResult, result);

        dataPackage.setDateAccessioned(customDate_2010_01_02);
        expResult = initialCount;
        result = instance.extract(journalName);
        assertEquals(expResult, result);
    }
}
