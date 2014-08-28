/*
 */
package org.datadryad.journalstatistics.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.test.ContextUnitTest;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileTotalSizeByDateTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(DataFileTotalSizeByDateTest.class);
    private File file;
    private Date date_2013_01_01, date_2013_06_01, date_2014_01_01, date_2013_03_01;

    @Before
    public void setUp() {
        super.setUp();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        date_2013_01_01 = calendar.getTime();
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        date_2013_03_01 = calendar.getTime();
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        date_2013_06_01 = calendar.getTime();
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.YEAR, 2014);
        date_2014_01_01 = calendar.getTime();

        try {
            file = new File(DataFileTotalSizeByDateTest.class.getClassLoader().getResource("DryadLogo.png").toURI());
        } catch (Exception ex) {
            fail("Exception setting up files for total size test " + ex);
        }
    }
    /**
     * Test of extract method, of class DataFileTotalSize.
     */
    @Test
    public void testCountDataFileTotalSizeByDate() throws Exception {
        log.info("countDataFileTotalSizeByDate");
        String journalName = "Test Journal";
        DataFileTotalSize instance1 = new DataFileTotalSize(context);
        instance1.setBeginDate(date_2013_01_01);
        instance1.setEndDate(date_2013_06_01);
        DataFileTotalSize instance2 = new DataFileTotalSize(context);
        instance2.setBeginDate(date_2013_06_01);
        instance2.setEndDate(date_2014_01_01);

        Long initialSize1 = instance1.extract(journalName);
        Long initialSize2 = instance2.extract(journalName);
        Long expectedSizeIncrease = file.length();

        assertTrue("file has no size", expectedSizeIncrease > 0);

        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        DryadDataFile dataFile = DryadDataFile.create(context, dataPackage);
        dataPackage.setPublicationName(journalName);
        dataFile.setDateAccessioned(date_2013_03_01);
        dataFile.addBitstream(new FileInputStream(file));

        Long totalSize1 = instance1.extract(journalName);
        Long sizeIncrease1 = totalSize1 - initialSize1;
        assertEquals("size increase mismatch",expectedSizeIncrease, sizeIncrease1);

        Long totalSize2 = instance2.extract(journalName);
        assertEquals("Size should not increase for outside date range", initialSize2, totalSize2);

    }
}
