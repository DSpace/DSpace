package org.dspace.workflow.actions.userassignment;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.PoolTask;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.actions.Action;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 6-aug-2010
 * Time: 16:31:21
 * To change this template use File | Settings | File Templates.
 */
public abstract class UserSelectionAction extends Action {

    protected static Logger log = Logger.getLogger(UserSelectionAction.class);

    public abstract boolean isFinished(WorkflowItem wfi);

    @Override
    public boolean isAuthorized(Context context, HttpServletRequest request, WorkflowItem wfi) throws SQLException {
        PoolTask task = null;
        if(context.getCurrentUser() != null)
            task = PoolTask.findByWorkflowIdAndEPerson(context, wfi.getID(), context.getCurrentUser().getID());

        //Check if we have pooled the current task
        //TODO: make sure that claimed tasks get removed if user is removed from group
        return task != null &&
                task.getWorkflowID().equals(getParent().getStep().getWorkflow().getID()) &&
                task.getStepID().equals(getParent().getStep().getId()) &&
                task.getActionID().equals(getParent().getId());
    }

    /**
     * Should a person have the option to repool the task the tasks will have to be regenerated
     * @param c the dspace context
     * @param wfi the workflowitem
     * @param userToIgnore the list of the to ignore users
     * @throws SQLException ...
     */
    public abstract void regenerateTasks(Context c, WorkflowItem wfi, List<Integer> userToIgnore) throws SQLException, AuthorizeException;

    public abstract boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException;
}
