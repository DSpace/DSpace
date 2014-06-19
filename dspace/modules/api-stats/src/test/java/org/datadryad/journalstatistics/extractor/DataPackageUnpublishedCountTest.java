/*
 */
package org.datadryad.journalstatistics.extractor;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
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
    private Date date_2014_06_07;
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
    }
    /**
     * Test of bucketForDate method, of class DataPackageUnpublishedCount.
     */
    @Test
    public void testBucketForDate() {
        System.out.println("bucketForDate");
        String expResult = STRING_2014_06;
        String result = DataPackageUnpublishedCount.bucketForDate(date_2014_06_07);
        assertEquals(expResult, result);
    }

    /**
     * Test of extractDateStringFromProvenance method, of class DataPackageUnpublishedCount.
     */
    @Test
    public void testExtractDateStringFromProvenance() throws Exception {
        System.out.println("extractDateStringFromProvenance");
        String provenance = PROVENANCE_MESSAGE;
        String expResult = BURIED_DATE_STRING;
        String result = DataPackageUnpublishedCount.extractDateStringFromProvenance(provenance);
        assertEquals(expResult, result);
    }

    @Test
    public void testCountUnpublishedDataPackages() throws Exception {
        System.out.println("countUnpublishedDataPackages");
        String journalName = "Test Journal";
        // Get the count prior to adding a data package to the workflow.
        DataPackageUnpublishedCount instance = new DataPackageUnpublishedCount(this.context);
        Map<String, Integer> results = instance.extract(journalName);
        Integer initialCount = 0;
        if(results != null && results.containsKey(STRING_2014_06)) {
            initialCount = results.get(STRING_2014_06);
        }
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
}
