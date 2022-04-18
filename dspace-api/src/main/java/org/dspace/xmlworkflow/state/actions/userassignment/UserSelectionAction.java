/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.Action;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An action that associates workflow users with tasks.
 * All the user selection actions must inherit from this action
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class UserSelectionAction extends Action {

    protected Logger log = org.apache.logging.log4j.LogManager.getLogger(UserSelectionAction.class);

    public abstract boolean isFinished(XmlWorkflowItem wfi);

    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;

    @Override
    public boolean isAuthorized(Context context, HttpServletRequest request, XmlWorkflowItem wfi)
        throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        PoolTask task = null;
        if (context.getCurrentUser() != null) {
            task = poolTaskService.findByWorkflowIdAndEPerson(context, wfi, context.getCurrentUser());
        }

        //Check if we have pooled the current task
        return task != null &&
            task.getWorkflowID().equals(getParent().getStep().getWorkflow().getID()) &&
            task.getStepID().equals(getParent().getStep().getId()) &&
            task.getActionID().equals(getParent().getId());
    }

    /**
     * Should a person have the option to repool the task the tasks will have to be regenerated
     *
     * @param c           the dspace context
     * @param wfi         the workflowitem
     * @param roleMembers the list of users for which tasks must be regenerated
     * @throws AuthorizeException thrown if the current user isn't authorized
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public abstract void regenerateTasks(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Check user constraints (enough users, group exists, ...).
     *
     * @param context the dspace context
     * @param wfi     the workflow item
     * @param hasUI   whether or not the Step has a user interface
     * @return if the action is valid
     * @throws WorkflowConfigurationException if there is an error in the workflow configuration.
     * @throws SQLException                   passed through.
     */
    public abstract boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI)
        throws WorkflowConfigurationException, SQLException;

    /**
     * A boolean indicating wether or not the task pool is used for this type of user selection
     *
     * @return a boolean
     */
    public abstract boolean usesTaskPool();
}
