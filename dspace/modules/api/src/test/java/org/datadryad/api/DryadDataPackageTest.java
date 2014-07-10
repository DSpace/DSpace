/*
 */
package org.datadryad.api;

import java.util.Set;
import org.datadryad.test.ContextUnitTest;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.core.Context;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataPackageTest extends ContextUnitTest {
    private static final String BURIED_DATE_STRING = "2014-03-05T18:11:29Z";
    private static final String PROVENANCE_MESSAGE = "Submitted by First Last (f.last@university.edu) on " + BURIED_DATE_STRING + " workflow start=Step: requiresReviewStep - action:noUserSelectionAction\nNo. of bitstreams: 0";

    /**
     * Test of getCollection method, of class DryadDataPackage.
     */
    @Test
    public void testGetCollection() throws Exception {
        Context context = this.context;
        Collection result = DryadDataPackage.getCollection(context);
        assertEquals(result.getName(), "Dryad Data Packages");
    }

    /**
     * Test of create method, of class DryadDataPackage.
     */
    @Test
    public void testCreate() throws Exception {
        System.out.println("create");
        DryadDataPackage result = DryadDataPackage.create(context);
        assertNotNull(result);
        assertNull(result.getWorkflowItem(context));
        assertNotNull(result.getIdentifier());
    }

    /**
     * Test of createInWorkflow method, of class DryadDataPackage
     */
    @Test
    public void testCreateInWorkflow() throws Exception {
        System.out.println("createInWorkflow");
        DryadDataPackage result = DryadDataPackage.createInWorkflow(context);
        assertNotNull(result);
        assertNotNull(result.getWorkflowItem(context));
        assertNotNull(result.getIdentifier());
    }

    /**
     * Test of makeSubmittedProvenance method, of class DryadDataPackage.
     */
    @Test
    public void testMakeSubmittedProvenance() {
        System.out.println("makeSubmittedProvenance");
        DCDate date = new DCDate(BURIED_DATE_STRING);
        String submitterName = "First Last";
        String submitterEmail = "f.last@university.edu";
        String provenanceStartId = "Step: requiresReviewStep - action:noUserSelectionAction";
        String bitstreamProvenanceMessage = "No. of bitstreams: 0";
        String expResult = PROVENANCE_MESSAGE;
        String result = DryadDataPackage.makeSubmittedProvenance(date, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        assertEquals(expResult, result);
    }

    /**
     * Test of getSubmittedProvenance method, of class DryadDataPackage.
     * Verifies that getSubmittedProvenance returns the most recent
     */
    @Test
    public void testGetSubmittedProvenance() throws Exception {
        System.out.println("getSubmittedProvenance");
        DCDate date1 = new DCDate(BURIED_DATE_STRING);
        String submitterName = "First Last";
        String submitterEmail = "f.last@university.edu";
        String provenanceStartId = "Step: requiresReviewStep - action:noUserSelectionAction";
        String bitstreamProvenanceMessage1 = "submission_1";
        DryadDataPackage instance = DryadDataPackage.create(context);
        instance.addSubmittedProvenance(date1, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage1);
        DCDate date2 = DCDate.getCurrent();
        String bitstreamProvenanceMessage2 = "submission_2";
        instance.addSubmittedProvenance(date2, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage2);
        String result = instance.getSubmittedProvenance();
        assertTrue(result.contains(bitstreamProvenanceMessage2));
        assertFalse(result.contains(bitstreamProvenanceMessage1));
    }

    /**
     * Test of addSubmittedProvenance method, of class DryadDataPackage.
     */
    @Test
    public void testAddSubmittedProvenance() throws Exception {
        System.out.println("addGetSubmittedProvenance");
        DCDate date = new DCDate(BURIED_DATE_STRING);
        String submitterName = "First Last";
        String submitterEmail = "f.last@university.edu";
        String provenanceStartId = "Step: requiresReviewStep - action:noUserSelectionAction";
        String bitstreamProvenanceMessage = "No. of bitstreams: 0";
        DryadDataPackage instance = DryadDataPackage.create(context);
        instance.addSubmittedProvenance(date, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        String result = instance.getSubmittedProvenance();
        String expResult = PROVENANCE_MESSAGE;
        assertEquals(expResult, result);
    }

    /**
     * Test of getPackagesContainingFile method, of class DryadDataPackage.
     */
    @Test
    public void testGetPackagesContainingFile() throws Exception {
        System.out.println("getPackagesContainingFile");
        DryadDataFile dataFile = DryadDataFile.create(context);
        DryadDataPackage dataPackage1 = DryadDataPackage.create(context);
        DryadDataPackage dataPackage2 = DryadDataPackage.create(context);
        dataPackage1.addDataFile(context, dataFile);
        Set result = DryadDataPackage.getPackagesContainingFile(context, dataFile);
        assertTrue(result.contains(dataPackage1));
        assertFalse(result.contains(dataPackage2));
        assertEquals(1, result.size());
    }

    /**
     * Test of getFilesInPackage method, of class DryadDataPackage.
     */
    @Test
    public void testGetFilesInPackage() throws Exception {
        System.out.println("getFilesInPackage");
        DryadDataFile dataFile1 = DryadDataFile.create(context);
        DryadDataFile dataFile2 = DryadDataFile.create(context);
        DryadDataFile dataFile3 = DryadDataFile.create(context);
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.addDataFile(context, dataFile1);
        dataPackage.addDataFile(context, dataFile2);
        Set result = DryadDataPackage.getFilesInPackage(context, dataPackage);
        assertTrue(result.contains(dataFile1));
        assertTrue(result.contains(dataFile2));
        assertFalse(result.contains(dataFile3));
    }

    /**
     * Test of getDataFiles method, of class DryadDataPackage.
     */
    @Test
    public void testGetDataFiles() throws Exception {
        System.out.println("getDataFiles");
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        DryadDataFile dataFile1 = DryadDataFile.create(context);
        Integer result = dataPackage.getDataFiles(context).size();
        Integer expResult = 1;
        assertEquals(expResult, result);
    }

    /**
     * Test of addDataFile method, of class DryadDataPackage.
     */
    @Test
    public void testAddDataFile() throws Exception {
        System.out.println("addDataFile");
        DryadDataFile dataFile = DryadDataFile.create(context);
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        Integer expectedNumberOfFiles = 0;
        Integer numberOfFiles = dataPackage.getDataFiles(context).size();
        assertEquals(expectedNumberOfFiles, numberOfFiles);
        dataPackage.addDataFile(context, dataFile);
        expectedNumberOfFiles = 1;
        numberOfFiles = dataPackage.getDataFiles(context).size();
        assertEquals(expectedNumberOfFiles, numberOfFiles);
    }

    @Test
    public void testMoveDataFile() throws Exception {
        System.out.println("moveDataFile");
        DryadDataFile dataFile = DryadDataFile.create(context);
        DryadDataPackage dataPackage1 = DryadDataPackage.create(context);
        DryadDataPackage dataPackage2 = DryadDataPackage.create(context);

        Integer expResult = 0;
        Integer result = dataPackage1.getDataFiles(context).size();
        assertEquals(expResult, result);
        dataPackage1.addDataFile(context, dataFile);
        expResult = 1;
        result = dataPackage1.getDataFiles(context).size();
        assertEquals(expResult, result);

        // Now move to package2 and make sure no longer in package 1
        dataPackage2.addDataFile(context, dataFile);
        expResult = 0;
        result = dataPackage1.getDataFiles(context).size();
        assertEquals(expResult, result);

        result = dataPackage2.getDataFiles(context).size();
        expResult = 1;
        assertEquals(expResult, result);
    }

    /**
     * Test of removeDataFile method, of class DryadDataPackage.
     */
    @Test
    public void testRemoveDataFile() throws Exception {
        System.out.println("removeDataFile");
        DryadDataFile dataFile = DryadDataFile.create(context);
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.addDataFile(context, dataFile);
        Integer expResult = 1;
        Integer result = dataPackage.getDataFiles(context).size();
        assertEquals(expResult, result);
        dataPackage.removeDataFile(dataFile);
        expResult = 0;
        result = dataPackage.getDataFiles(context).size();
        assertEquals(expResult, result);
    }

    /**
     * Test of indexOfValue method, of class DryadDataPackage.
     */
    @Test
    public void testIndexOfValue() {
        System.out.println("indexOfValue");
        Integer numValues = 4;
        DCValue[] dcValues = new DCValue[numValues];
        for(Integer i=0;i<numValues;i++) {
            dcValues[i] = new DCValue();
            dcValues[i].value = String.format("Value %d", i * 100);
        }

        // "Value 200" should be at index 3
        String value = "Value 200";
        Integer expResult = 3;
        Integer result = DryadDataPackage.indexOfValue(dcValues, value);
        assertEquals(expResult, result);
    }

    /**
     * Test of setPublicationName method, of class DryadDataPackage.
     */
    @Test
    public void testSetPublicationName() throws Exception {
        System.out.println("setPublicationName");
        String publicationName = "Test Publication";
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(publicationName);
    }
}
