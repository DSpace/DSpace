/*
 */
package org.datadryad.api;

import org.datadryad.test.ContextUnitTest;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
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
    public void testAddGetSubmittedProvenance() throws Exception {
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
}
