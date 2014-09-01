/*
 */
package org.datadryad.journalstatistics.extractor;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.test.ContextUnitTest;
import org.dspace.content.DCDate;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageUnpublishedCountTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(DataPackageUnpublishedCountTest.class);
    private Date date_2014_06_07, date_2014_06_01, date_2014_06_15, date_2014_06_30;
    private static final String STRING_2014_06 = "2014-06";
    private static final String BURIED_DATE_STRING = "2014-03-05T18:11:29Z";
    private static final String PROVENANCE_MESSAGE = "Submitted by First Last (f.last@university.edu) on " + BURIED_DATE_STRING + " workflow start=Step: requiresReviewStep - action:noUserSelectionAction\nNo. of bitstreams: 0";
    
    @Before
    @Override
    public void setUp() {
        super.setUp();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2014);
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        date_2014_06_07 = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        date_2014_06_01 = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 15);
        date_2014_06_15 = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        date_2014_06_30 = calendar.getTime();
    }
    /**
     * Test of bucketForDate method, of class DataPackageUnpublishedCount.
     */
    @Test
    public void testBucketForDate() {
        log.info("bucketForDate");
        String expResult = STRING_2014_06;
        String result = DataPackageUnpublishedCount.bucketForDate(date_2014_06_07);
        assertEquals(expResult, result);
    }

    /**
     * Test of extractDateStringFromProvenance method, of class DataPackageUnpublishedCount.
     */
    @Test
    public void testExtractDateStringFromProvenance() throws Exception {
        log.info("extractDateStringFromProvenance");
        String provenance = PROVENANCE_MESSAGE;
        String expResult = BURIED_DATE_STRING;
        String result = DataPackageUnpublishedCount.extractDateStringFromProvenance(provenance);
        assertEquals(expResult, result);
    }

    @Test
    public void testCountUnpublishedDataPackages() throws Exception {
        log.info("countUnpublishedDataPackages");
        String journalName = "Test Journal";
        // Get the count prior to adding a data package to the workflow.
        DataPackageUnpublishedCount instance = new DataPackageUnpublishedCount(this.context);
        Map<String, Integer> results = instance.extract(journalName);
        Integer initialCount = DataPackageUnpublishedCount.getCountOrZero(results, STRING_2014_06);
        DryadDataPackage dataPackage = DryadDataPackage.createInWorkflow(context);
        DCDate submittedDate = new DCDate(date_2014_06_07);
        String submitterName = "Cornelius Tester";
        String submitterEmail = "test@example.com";
        String provenanceStartId = "TEST START ID";
        String bitstreamProvenanceMessage = "Number of Bitstreams: 0";
        dataPackage.setPublicationName(journalName);
        dataPackage.addSubmittedProvenance(submittedDate, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        results = instance.extract(journalName);
        assertNotNull("Result map should be non-null", results);
        assertTrue("Results should contain a count for " + STRING_2014_06, results.containsKey(STRING_2014_06));
        Integer expectedCount = initialCount + 1;
        Integer resultCount = results.get(STRING_2014_06);
        assertEquals("Count mismatch", expectedCount, resultCount);

        // Add a package to the archive with submission metadata
        // and make sure it is not counted
        dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(journalName);
        dataPackage.addSubmittedProvenance(submittedDate, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        results = instance.extract(journalName);
        assertNotNull("Result map should be non-null", results);
        assertTrue("Results should contain a count for " + STRING_2014_06, results.containsKey(STRING_2014_06));
        resultCount = results.get(STRING_2014_06);
        assertEquals("Count mismatch", expectedCount, resultCount);
    }


    @Test
    public void testCountUnpublishedDataPackagesInDateRange() throws Exception {
        log.info("countUnpublishedDataPackagesInDateRange");
        // Make two instances with different ranges
        // add a data package to one range. Assert it increases and the other does not
        DataPackageUnpublishedCount instance1 = new DataPackageUnpublishedCount(this.context);
        instance1.setBeginDate(date_2014_06_01);
        instance1.setEndDate(date_2014_06_15);
        DataPackageUnpublishedCount instance2 = new DataPackageUnpublishedCount(this.context);
        instance2.setBeginDate(date_2014_06_15);
        instance2.setEndDate(date_2014_06_30);
        // date_2014_06_07 is in range 1 but not range 2

        String journalName = "Test Journal";
        Map<String, Integer> results1 = instance1.extract(journalName);
        Map<String, Integer> results2 = instance2.extract(journalName);

        Integer initialCount1 = DataPackageUnpublishedCount.getCountOrZero(results1, STRING_2014_06);
        Integer initialCount2 = DataPackageUnpublishedCount.getCountOrZero(results2, STRING_2014_06);

        // now add a package with provenance for middle of june, and make sure instance2 doesnt' change
        DryadDataPackage dataPackage = DryadDataPackage.createInWorkflow(context);
        DCDate submittedDate = new DCDate(date_2014_06_07);
        String submitterName = "Cornelius Tester";
        String submitterEmail = "test@example.com";
        String provenanceStartId = "TEST START ID";
        String bitstreamProvenanceMessage = "Number of Bitstreams: 0";
        dataPackage.setPublicationName(journalName);
        dataPackage.addSubmittedProvenance(submittedDate, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);

        results1 = instance1.extract(journalName);
        results2 = instance2.extract(journalName);

        Integer finalCount1 = DataPackageUnpublishedCount.getCountOrZero(results1, STRING_2014_06);
        Integer finalCount2 = DataPackageUnpublishedCount.getCountOrZero(results2, STRING_2014_06);

        // count1 should have increased
        // count2 should not

        Integer countIncrease1 = finalCount1 - initialCount1;
        Integer countIncrease2 = finalCount2 - initialCount2;

        assertTrue("Count 1 should have increased", countIncrease1 > 0);
        assertTrue("Count 2 should not have increased", countIncrease2 == 0);
    }
}
