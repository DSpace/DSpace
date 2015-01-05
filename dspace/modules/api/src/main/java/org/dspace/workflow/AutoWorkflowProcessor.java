/*
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.actions.WorkflowActionConfig;

/**
 * Class to move item through workflow, without user interaction.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AutoWorkflowProcessor {
    private static final Logger log = Logger.getLogger(AutoWorkflowProcessor.class);
    private final Context context;
    private WorkflowItem wfi;
    private DryadDataPackage dataPackage;
    private PoolTask poolTask;
    private ClaimedTask claimedTask;
    public AutoWorkflowProcessor(Context context) {
        this.context = context;
    }

    // read-only accessors for subclasses
    protected Context getContext() { return context; }
    protected WorkflowItem getWfi() { return wfi; }
    protected DryadDataPackage getDataPackage() { return dataPackage; }
    protected PoolTask getPoolTask() { return poolTask; }
    protected ClaimedTask getClaimedTask() { return claimedTask; }

    // Utility methods
    private static Boolean isClaimed(Context c, WorkflowItem wfi) throws SQLException {
        List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
        // If there are claimed tasks for this workflow item, it is claimed
        return !claimedTasks.isEmpty();
    }

    static EPerson getSystemCurator(Context c)  throws AutoWorkflowProcessorException, SQLException {
        try {
            String email = ConfigurationManager.getProperty("workflow", "system.curator.account");
            if(email == null) {
                throw new AutoWorkflowProcessorException("system.curator.email is not present in config/workflow.cfg, cannot process batches");
            }
            EPerson systemCurator = EPerson.findByEmail(c, email);
            return systemCurator;
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("Authorize exception finding system curator", ex);
        }
    }

    private Boolean isMyTask() {
        return getClaimedTask().getActionID().equals(getActionID());
    }
    
    abstract Boolean isMyStep(final String stepId) throws SQLException;
    abstract String getActionID();
    // After claimed, look at whatever is needed to determine if the task should be processed
    abstract Boolean canProcessClaimedTask() throws SQLException;

    // The XMLUI workflow classes inspect an HttpServletRequest object for
    // parameters, such as which button a user clicked or which form was served.
    // WorkflowManager.doState() relies on this, so the individual processors
    // must suppy them, even if they're mostly static values.
    abstract HttpServletRequest getRequest();

    private static void deleteClaimedTask(Context c, WorkflowItem wfi, ClaimedTask claimedTask) throws SQLException, AutoWorkflowProcessorException {
        try {
            WorkflowManager.deleteClaimedTask(c, wfi, claimedTask);
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("Unable to delete claimed task", ex);
        }
    }

    public final Boolean processWorkflowItem(Integer wfItemId) throws AutoWorkflowProcessorException, SQLException, ItemIsNotEligibleForStepException {
        try {
            WorkflowItem workflowItem = WorkflowItem.find(this.context, wfItemId);
            if(workflowItem == null) {
                throw new AutoWorkflowProcessorException("Workflow item ID: " + wfItemId + " not found in workflow");
            } else {
                return processWorkflowItem(workflowItem);
            }
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("Authorize exception finding workflowitem " + wfItemId + " in workflow", ex);
        } catch (IOException ex) {
            throw new AutoWorkflowProcessorException("IO exception finding workflowitem " + wfItemId + " in workflow", ex);
        }
    }

    public final Boolean processWorkflowItem(WorkflowItem wfi) throws SQLException, AutoWorkflowProcessorException, ItemIsNotEligibleForStepException {
        this.context.setCurrentUser(getSystemCurator(this.context));
        if(wfi == null) {
            throw new AutoWorkflowProcessorException("Cannot process null item");
        } else if(getContext() == null) {
            throw new AutoWorkflowProcessorException("Cannot process item with null context");
        }
        this.wfi = wfi;
        DryadDataPackage aDataPackage = new DryadDataPackage(wfi.getItem());
        if(aDataPackage == null) {
            throw new AutoWorkflowProcessorException("Unable to find data package for item " + wfi.getItem());
        }
        this.dataPackage = aDataPackage;

        // Item must not already be claimed
        if(isClaimed(getContext(), wfi)) {
            throw new AutoWorkflowProcessorException("Cannot process item that is already claimed by a user");
        }

        // Must have a task in the pool for this user
        EPerson eperson = getSystemCurator(getContext());
        if(eperson == null) {
            throw new AutoWorkflowProcessorException("Cannot get system curator process workflow item");
        }
        
        PoolTask aPoolTask = PoolTask.findByWorkflowIdAndEPerson(getContext(), wfi.getID(), eperson.getID());
        if(aPoolTask == null) {
            // Task
            throw new AutoWorkflowProcessorException("Cannot find task to claim for wfi: " + wfi.getID() + " ePersonID:" + eperson.getID() + ". Verify the item is ready to be claimed and that the eperson has a row in tasklistitem");
        }
        this.poolTask = aPoolTask;
        // Before claiming, make sure the task what we can process

        if(!isMyStep(aPoolTask.getStepID())) {
            // the step to claim is not our step, abort
            throw new ItemIsNotEligibleForStepException("Task for wfi: " + wfi.getID() + " ePersonID: " + eperson.getID() + " is not in the correct step for this processor, not claiming");
        }

        Workflow workflow = null;
        Step step = null;
        WorkflowActionConfig action = null;

        try {
            workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
            step = workflow.getStep(aPoolTask.getStepID());
            action = step.getActionConfig(getActionID());
            // This method does not return the created task, so it must be fetched separately
            WorkflowManager.createOwnedTask(getContext(), wfi, step, action, eperson);
        } catch (IOException ex) {
            throw new AutoWorkflowProcessorException("IOException getting workflow", ex);
        } catch (WorkflowConfigurationException ex) {
            throw new AutoWorkflowProcessorException("WorkflowConfigurationException getting workflow", ex);
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("AuthorizeException creating task", ex);
        }

        // Fetch the just-claimed task - wfi and eperson should match
        ClaimedTask aClaimedTask = ClaimedTask.findByWorkflowIdAndEPerson(getContext(), wfi.getID(), eperson.getID());

        if(aClaimedTask == null) {
            // This is truly exceptional - we successfully created a task but couldn't find it
            throw new AutoWorkflowProcessorException("Unable to find just-claimed task for wfi: " + wfi.getID() + " ePersonID:" + eperson.getID());
        }
        this.claimedTask = aClaimedTask;

        if(!isMyTask()) {
            // We claimed a task but it's not the task we can process
            deleteClaimedTask(getContext(), wfi, aClaimedTask);
            throw new AutoWorkflowProcessorException("Just-claimed task is NOT the correct task, serious internal error");
        }

        if(!canProcessClaimedTask()) {
            deleteClaimedTask(getContext(), wfi, aClaimedTask);
            return Boolean.FALSE;
        }

        // At this point, we've checked that we can process the task, process it!

        try {
            // WorkflowManager.doState: "Executes an action and returns the next."
            WorkflowManager.doState(getContext(), eperson, getRequest(), aClaimedTask.getWorkflowItemID(), workflow, action);
        } catch (IOException ex) {
            throw new AutoWorkflowProcessorException("IOException processing action", ex);
        } catch (WorkflowConfigurationException ex) {
            throw new AutoWorkflowProcessorException("WorkflowConfigurationException processing action", ex);
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("AuthorizeException processing action", ex);
        } catch (MessagingException ex) {
            throw new AutoWorkflowProcessorException("MessagingException processing action", ex);
        } catch (WorkflowException ex) {
            throw new AutoWorkflowProcessorException("WorkflowException processing action", ex);
        }

        return Boolean.TRUE;
    }
}
