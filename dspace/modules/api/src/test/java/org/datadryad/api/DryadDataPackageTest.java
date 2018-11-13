/*
 */
package org.datadryad.api;

import java.util.Set;
import org.apache.log4j.Logger;
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
    private static Logger log = Logger.getLogger(DryadDataPackageTest.class);
    private static final String BURIED_DATE_STRING = "2014-03-05T18:11:29Z";
    private static final String PROVENANCE_MESSAGE = "Submitted by First Last (f.last@university.edu) on " + BURIED_DATE_STRING + " workflow start=Step: requiresReviewStep - action:noUserSelectionAction\nNo. of bitstreams: 0";

    /**
     * Test of getCollection method, of class DryadDataPackage.
     */
    @Test
    public void testGetCollection() throws Exception {
        log.info("getCollection");
        Context context = this.context;
        Collection result = DryadDataPackage.getCollection(context);
        assertEquals(result.getName(), "Dryad Data Packages");
    }

    /**
     * Test of create method, of class DryadDataPackage.
     */
    @Test
    public void testCreate() throws Exception {
        log.info("create");
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
        log.info("createInWorkflow");
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
        log.info("makeSubmittedProvenance");
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
        log.info("getSubmittedProvenance");
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
        log.info("addGetSubmittedProvenance");
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
        log.info("getPackagesContainingFile");
        DryadDataPackage dataPackage1 = DryadDataPackage.create(context);
        DryadDataPackage dataPackage2 = DryadDataPackage.create(context);
        DryadDataFile dataFile = DryadDataFile.create(context, dataPackage1);
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
        log.info("getFilesInPackage");
        DryadDataPackage dataPackage1 = DryadDataPackage.create(context);
        DryadDataPackage dataPackage2 = DryadDataPackage.create(context);

        DryadDataFile dataFile1 = DryadDataFile.create(context, dataPackage1);
        DryadDataFile dataFile2 = DryadDataFile.create(context, dataPackage1);
        DryadDataFile dataFile3 = DryadDataFile.create(context, dataPackage2);
        dataPackage1.addDataFile(context, dataFile1);
        dataPackage1.addDataFile(context, dataFile2);
        Set result = DryadDataPackage.getFilesInPackage(context, dataPackage1);
        assertTrue(result.contains(dataFile1));
        assertTrue(result.contains(dataFile2));
        assertFalse(result.contains(dataFile3));
    }

    /**
     * Test of getDataFiles method, of class DryadDataPackage.
     */
    @Test
    public void testGetDataFiles() throws Exception {
        log.info("getDataFiles");
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        DryadDataFile dataFile1 = DryadDataFile.create(context, dataPackage);
        Integer result = dataPackage.getDataFiles(context).size();
        Integer expResult = 1;
        assertEquals(expResult, result);
    }

    @Test
    public void testMoveDataFile() throws Exception {
        log.info("moveDataFile");
        DryadDataPackage dataPackage1 = DryadDataPackage.create(context);
        DryadDataPackage dataPackage2 = DryadDataPackage.create(context);
        DryadDataFile dataFile = DryadDataFile.create(context, dataPackage1);

        Integer expResult = 1;
        Integer result = dataPackage1.getDataFiles(context).size();
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
        log.info("removeDataFile");
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        DryadDataFile dataFile = DryadDataFile.create(context, dataPackage);
        Integer expResult = 1;
        Integer result = dataPackage.getDataFiles(context).size();
        assertEquals(expResult, result);
        dataPackage.removeDataFile(context, dataFile);
        expResult = 0;
        result = dataPackage.getDataFiles(context).size();
        assertEquals(expResult, result);
    }
    
    /**
     * Test of setPublicationName method, of class DryadDataPackage.
     */
    @Test
    public void testSetPublicationName() throws Exception {
        log.info("setPublicationName");
        String publicationName = "Test Publication";
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        dataPackage.setPublicationName(publicationName);
    }
}
