package org.dspace.workflow.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.Action;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 13-aug-2010
 * Time: 14:43:18
 */
public abstract class ProcessingAction extends Action {
    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException, IOException, WorkflowException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAuthorized(Context context, HttpServletRequest request, WorkflowItem wfi) throws SQLException {
        ClaimedTask task = null;
        if(context.getCurrentUser() != null)
            task = ClaimedTask.findByWorkflowIdAndEPerson(context, wfi.getID(), context.getCurrentUser().getID());
        //Check if we have claimed the current task
        //TODO: make sure that claimed tasks get removed if user is removed from group
        return task != null &&
                task.getWorkflowID().equals(getParent().getStep().getWorkflow().getID()) &&
                task.getStepID().equals(getParent().getStep().getId()) &&
                task.getActionID().equals(getParent().getId());
    }
}
