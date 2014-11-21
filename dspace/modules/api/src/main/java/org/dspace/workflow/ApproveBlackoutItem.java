/*
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.mail.MessagingException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.actions.WorkflowActionConfig;

/**
 * Class to approve an item in Publication Blackout, without user interaction.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ApproveBlackoutItem {
    private static final Logger log = Logger.getLogger(ApproveBlackoutItem.class);

    private static Boolean isClaimed(Context c, WorkflowItem wfi) throws SQLException {
        List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
        // If there are claimed tasks for this workflow item, it is claimed
        return !claimedTasks.isEmpty();
    }

    private static EPerson getSystemCurator(Context c)  throws ApproveBlackoutItemException, SQLException {
        try {
            String email = ConfigurationManager.getProperty("workflow", "system.curator.email");
            if(email == null) {
                throw new ApproveBlackoutItemException("system.curator.email is not present in config/workflow.cfg, cannot process batches");
            }
            EPerson systemCurator = EPerson.findByEmail(c, email);
            return systemCurator;
        } catch (AuthorizeException ex) {
            throw new ApproveBlackoutItemException("Authorize exception finding system curator", ex);
        }
    }

    public static void main(String args[]) throws ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

	options.addOption("i", "wfitemid", true, "workflowitem id for an unclaimed item in "
                + "'pendingPublicationStep' or 'pendingPublicationReauthorizationPaymentStep'.\n"
                + "Item must have a dc.date.blackoutUntil metadata value in the past.");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ApproveItemBlackout\n", options);
        }

        if(line.hasOption('i')) {
	    // get a WorkflowItem using a workflow ID
	    Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            int result = 0;
            try {
                c = new Context();
                c.setCurrentUser(getSystemCurator(c));
                if(approveBlackoutItem(c, wfItemId)) {
                    System.out.println("Successfully approved workflowitem " + wfItemId + " from blackout");
                } else {
                    System.out.println("Did not lift blackout on " + wfItemId + ", check logs for details");
                }
            } catch (ApproveBlackoutItemException ex) {
                System.err.println("Exception approving blackout item: " + ex);
                result = 1;
            } catch (ItemIsNotInBlackoutException ex) {
                System.err.println("Item is not in blackout: " + ex);
                result = 1;
            } catch (SQLException ex) {
                System.err.println("Exception approving blackout item: " + ex);
                result = 1;
            } finally {
                if(c != null) {
                    try {
                        c.complete();
                    } catch (SQLException ex) {

                    }
                }
            }
            System.exit(result);
	} else {
            System.out.println("No workflow ID was given. This must be provided to identify the item in the workflow");
            System.exit(1);
        }
    }

    // Make it testable!
    public static Boolean approveBlackoutItem(Context c, Integer wfItemId) throws ApproveBlackoutItemException, SQLException, ItemIsNotInBlackoutException {
        try {
            WorkflowItem wfi = WorkflowItem.find(c, wfItemId);
            if(wfi == null) {
                throw new ApproveBlackoutItemException("Workflow item ID: " + wfItemId + " not found in workflow");
            } else {
                return approveBlackoutItem(c, wfi);
            } 
        } catch (AuthorizeException ex) {
            throw new ApproveBlackoutItemException("Authorize exception finding workflowitem " + wfItemId + " in workflow", ex);
        } catch (IOException ex) {
            throw new ApproveBlackoutItemException("IO exception finding workflowitem " + wfItemId + " in workflow", ex);
        }
    }

    private static Boolean isBlackoutApproveTask(ClaimedTask t) {
        return t.getActionID().equals("afterPublicationAction");
    }

    private static Boolean isBlackoutApproveStep(PoolTask p) throws SQLException {
        return (p.getStepID().equals("pendingPublicationStep") ||
                p.getStepID().equals("pendingPublicationReAuthorizationPaymentStep"));
    }

    private static void deleteClaimedTask(Context c, WorkflowItem wfi, ClaimedTask claimedTask) throws SQLException, ApproveBlackoutItemException {
        try {
            WorkflowManager.deleteClaimedTask(c, wfi, claimedTask);
        } catch (AuthorizeException ex) {
            throw new ApproveBlackoutItemException("Unable to delete claimed task", ex);
        }
    }

    // TODO: Implement a caller
    // Should create the context and close it at the end
    private static Boolean approveBlackoutItem(Context c, WorkflowItem wfi) throws SQLException, ApproveBlackoutItemException, ItemIsNotInBlackoutException {
        if(wfi == null) {
            throw new ApproveBlackoutItemException("Cannot approve null item");
        } else if(c == null) {
            throw new ApproveBlackoutItemException("Cannot approve item with null context");
        }

        DryadDataPackage dataPackage = new DryadDataPackage(wfi.getItem());
        if(dataPackage == null) {
            throw new ApproveBlackoutItemException("Unable to find data package for item " + wfi.getItem());
        }

        // Item must not already be claimed
        if(isClaimed(c, wfi)) {
            throw new ApproveBlackoutItemException("Cannot approve item that is already claimed by a user");
        }

        // Must have a task in the pool for this user
        EPerson eperson = getSystemCurator(c);
        if(eperson == null) {
            throw new ApproveBlackoutItemException("Cannot get system curator to approve blackout item");
        }
        PoolTask poolTask = PoolTask.findByWorkflowIdAndEPerson(c, wfi.getID(), eperson.getID());
        if(poolTask == null) {
            // Task
            throw new ApproveBlackoutItemException("Cannot find task to claim for wfi: " + wfi.getID() + " ePersonID:" + eperson.getID() + ". Verify the item is ready to be claimed and that the eperson has a row in tasklistitem");
        }

        // Before claiming, make sure the task is a blackout approval
        // We don't handle anything else

        if(!isBlackoutApproveStep(poolTask)) {
            // the step to claim is not blackout, abort
            throw new ItemIsNotInBlackoutException("Task for wfi: " + wfi.getID() + " ePersonID: " + eperson.getID() + " is not a blackout task - item is not in blackout, not claiming");
        }

        Workflow workflow = null;
        Step step = null;
        WorkflowActionConfig action = null;

        try {
            workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
            step = workflow.getStep(poolTask.getStepID());
            action = step.getActionConfig("afterPublicationAction");
            // This method does not return the created task, so it must be fetched separately
            WorkflowManager.createOwnedTask(c, wfi, step, action, eperson);
        } catch (IOException ex) {
            throw new ApproveBlackoutItemException("IOException getting workflow", ex);
        } catch (WorkflowConfigurationException ex) {
            throw new ApproveBlackoutItemException("WorkflowConfigurationException getting workflow", ex);
        } catch (AuthorizeException ex) {
            throw new ApproveBlackoutItemException("AuthorizeException creating task", ex);
        }

        // Fetch the just-claimed task - wfi and eperson should match
        ClaimedTask claimedTask = ClaimedTask.findByWorkflowIdAndEPerson(c, wfi.getID(), eperson.getID());

        if(claimedTask == null) {
            // This is truly exceptional - we successfully created a task but couldn't find it
            throw new ApproveBlackoutItemException("Unable to find just-claimed task for wfi: " + wfi.getID() + " ePersonID:" + eperson.getID());
        }

        if(!isBlackoutApproveTask(claimedTask)) {
            // We claimed a task but it's not a blackout approve task
            deleteClaimedTask(c, wfi, claimedTask);
            throw new ApproveBlackoutItemException("Just-claimed task is NOT a blackout task, serious internal error");
        }

        // Verify date is in the past
        Date now = new Date();
        Date blackoutUntilDate = dataPackage.getBlackoutUntilDate();
        if(blackoutUntilDate == null) {
            log.error("Attempted to lift blackout on item: " + wfi.getItem().getID() + " but no blackoutUntilDate present");
            // Too early, delete the task
            deleteClaimedTask(c, wfi, claimedTask);
            return Boolean.FALSE;
        }

        if(now.before(blackoutUntilDate)) {
            // current date is before the blackout until date
            log.error("Attempted to lift blackout early on item " + wfi.getItem().getID() +
                    ". Current date: " + now + " blackoutUntilDate: " + blackoutUntilDate);
            deleteClaimedTask(c, wfi, claimedTask);
            return Boolean.FALSE;
        }

        // At this point, correct task is claimed and the blackout date is in the past.
        // Just need to execute it

        try {
            // WorkflowManager.doState: "Executes an action and returns the next."
            WorkflowManager.doState(c, eperson, null, claimedTask.getWorkflowItemID(), workflow, action);
        } catch (IOException ex) {
            throw new ApproveBlackoutItemException("IOException approving out of blackout", ex);
        } catch (WorkflowConfigurationException ex) {
            throw new ApproveBlackoutItemException("WorkflowConfigurationException approving out of blackout", ex);
        } catch (AuthorizeException ex) {
            throw new ApproveBlackoutItemException("AuthorizeException approving out of blackout", ex);
        } catch (MessagingException ex) {
            throw new ApproveBlackoutItemException("MessagingException approving out of blackout", ex);
        } catch (WorkflowException ex) {
            throw new ApproveBlackoutItemException("WorkflowException approving out of blackout", ex);
        }

        // TODO: task to find eligible items in the workflow and approve them
        return Boolean.TRUE;
    }
}
