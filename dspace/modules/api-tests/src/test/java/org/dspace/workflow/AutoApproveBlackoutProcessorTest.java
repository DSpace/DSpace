/*
 */
package org.dspace.workflow;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.test.ContextUnitTest;
import org.dspace.content.WorkspaceItem;
import org.dspace.doi.DryadDOIRegistrationHelper;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.theories.Theory;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AutoApproveBlackoutProcessorTest extends ContextUnitTest {
    private static final Logger log = Logger.getLogger(AutoApproveBlackoutProcessorTest.class);
    private Calendar calendar;
    private Date pastDate;
    private Date nowDate;
    private Date futureDate;

    @Before
    public void setUp() {
        super.setUp();
        calendar = new GregorianCalendar();
        nowDate = calendar.getTime();
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.set(Calendar.YEAR, currentYear - 1);
        pastDate = calendar.getTime();
        calendar.set(Calendar.YEAR, currentYear + 1);
        futureDate = calendar.getTime();
    }

    private void enableJournalSubscription(DryadDataPackage dataPackage) throws Exception {
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
        ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(context,dataPackage.getItem().getID());
        shoppingCart.setJournalSub(true);
        shoppingCart.update();
    }

    /**
     * Creates a bare data package in the workspace and updates its shopping
     * cart record to suppress payment processing.
     * @return A data package, with submitter set to the 'system curator'
     * (see {@link AutoWorkflowProcessor#getSystemCurator(org.dspace.core.Context) })
     * @throws Exception 
     */
    private DryadDataPackage createDataPackage() throws Exception {
        // Payment system cannot create a shopping cart unless
        // context.currentUser is set
        EPerson systemCurator = AutoWorkflowProcessor.getSystemCurator(context);
        context.setCurrentUser(systemCurator);

        DryadDataPackage dataPackage = DryadDataPackage.createInWorkspace(context);
        // ReviewStep email fails if no submitter, so we need to set one.
        dataPackage.getItem().setSubmitter(AutoWorkflowProcessor.getSystemCurator(context));

        // Current workflow moves items through payment processing steps/actions
        // To prevent the item from requiring real payment processing, we update
        // its shopping cart record as soon as it exists.
        enableJournalSubscription(dataPackage);
        return dataPackage;
    }

    /**
     * Tests that an item in blackout can be approved.
     * Creates a bare item in the workspace and invokes two processors to
     * send it to blackout then archive. Works around payment system by
     * setting all shopping carts with journal subscription = true
     * @throws Exception
     */
    @Test
    public void testApproveFromBlackoutWithPastDate() throws Exception {
        DryadDataPackage dataPackage = createDataPackage();
        // Uses WorkflowManager.start(), which invokes the entire workflow process
        // Other tests that use DryadDataPackage simply createInWorkflow, as
        // they do not need the entire set of tasks/steps.
        WorkspaceItem wsi = dataPackage.getWorkspaceItem(context);
        WorkflowManager.start(context, wsi);

        AutoCurateToBlackoutProcessor curateToBlackoutProcessor = new AutoCurateToBlackoutProcessor(context);
        Boolean curatedToBlackout = curateToBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertTrue("Could not curate new item to blackout", curatedToBlackout);

        // Now, item is in blackout, test that it can be approved
        dataPackage.setBlackoutUntilDate(pastDate); // Set the blackoutUntil date to be the past

        Boolean inBlackout = DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(dataPackage.getItem());
        assertTrue("Item should be in blackout", inBlackout);

        AutoApproveBlackoutProcessor approveBlackoutProcessor = new AutoApproveBlackoutProcessor(context);
        Boolean approvedFromBlackout = approveBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertTrue("Could not approve item from blackout to archive", approvedFromBlackout);

        Boolean archived = dataPackage.getItem().isArchived();
        assertTrue("Item should be archived", archived);
    }

    /**
     * Tests that an item in blackout will not be approved when its blackoutUntil
     * date is in the future.
     * @throws Exception
     */
    @Test
    public void testApproveFromBlackoutFailsWithFutureDate() throws Exception {
        DryadDataPackage dataPackage = createDataPackage();
        WorkspaceItem wsi = dataPackage.getWorkspaceItem(context);
        WorkflowManager.start(context, wsi);

        AutoCurateToBlackoutProcessor curateToBlackoutProcessor = new AutoCurateToBlackoutProcessor(context);
        Boolean curatedToBlackout = curateToBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertTrue("Could not curate new item to blackout", curatedToBlackout);

        dataPackage.setBlackoutUntilDate(futureDate); // Set the blackoutUntil date to be the past

        // Now, item is in blackout, verify it will not be approved
        Boolean inBlackout = DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(dataPackage.getItem());
        assertTrue("Item should be in blackout", inBlackout);

        AutoApproveBlackoutProcessor approveBlackoutProcessor = new AutoApproveBlackoutProcessor(context);
        Boolean approvedFromBlackout = approveBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertFalse("Incorrectly approved item with future blackoutUntil date", approvedFromBlackout);

        Boolean archived = dataPackage.getItem().isArchived();
        assertFalse("Item should not be archived", archived);
    }

    /**
     * Tests that an item in blackout will not be approved when it has no
     * blackoutUntil date
     * @throws Exception
     */
    @Test
    public void testApproveFromBlackoutFailsWithNoDate() throws Exception {
        DryadDataPackage dataPackage = createDataPackage();
        WorkspaceItem wsi = dataPackage.getWorkspaceItem(context);
        WorkflowManager.start(context, wsi);

        AutoCurateToBlackoutProcessor curateToBlackoutProcessor = new AutoCurateToBlackoutProcessor(context);
        Boolean curatedToBlackout = curateToBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertTrue("Could not curate new item to blackout", curatedToBlackout);

        Boolean inBlackout = DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(dataPackage.getItem());
        assertTrue("Item should be in blackout", inBlackout);

        // Now, item is in blackout, make sure it has no blackoutUntil date and verify it will not be approved
        Date blackoutUntilDate = dataPackage.getBlackoutUntilDate();
        assertNull("Item should have no blackoutUntil Date", blackoutUntilDate);

        AutoApproveBlackoutProcessor approveBlackoutProcessor = new AutoApproveBlackoutProcessor(context);
        Boolean approvedFromBlackout = approveBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertFalse("Incorrectly approved item with no blackoutUntil date", approvedFromBlackout);

        Boolean archived = dataPackage.getItem().isArchived();
        assertFalse("Item should not be archived", archived);
    }

    /**
     * Tests that an item in blackout will not be approved when the item is not
     * actually in blackout.
     * @throws Exception
     */
    @Test(expected = ItemIsNotEligibleForStepException.class)
    public void testApproveFromBlackoutFailsNotInBlackout() throws Exception {
        DryadDataPackage dataPackage = createDataPackage();
        WorkspaceItem wsi = dataPackage.getWorkspaceItem(context);
        WorkflowManager.start(context, wsi);
        Boolean inBlackout = DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(dataPackage.getItem());
        assertFalse("Item should not be in blackout", inBlackout);
        AutoApproveBlackoutProcessor approveBlackoutProcessor = new AutoApproveBlackoutProcessor(context);
        Boolean approvedFromBlackout = approveBlackoutProcessor.processWorkflowItem(dataPackage.getWorkflowItem(context));
        assertFalse("ApproveBlackoutProcessor was able to approve an item not in blackout", approvedFromBlackout);

        Boolean archived = dataPackage.getItem().isArchived();
        assertFalse("Item should not be archived", archived);
    }

    /**
     * Test of getActionID method, of class AutoApproveBlackoutProcessor.
     */
    @Test
    public void testGetActionID() {
        log.info("getActionID");
        AutoApproveBlackoutProcessor instance = new AutoApproveBlackoutProcessor(context);
        String expResult = "afterPublicationAction";
        String result = instance.getActionID();
        assertEquals("Blackout processor's action mismatch", expResult, result);
    }

}
