/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * WorkflowItem table pointing to it.
 *
 * Once the item has completed the workflow it will be archived.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface XmlWorkflowService extends WorkflowService<XmlWorkflowItem> {

    /**
     * Send an email to some addresses, concerning a WorkflowItem, using a given
     * template.
     *
     * @param c current DSpace session.
     * @param wfi the workflow item.
     * @param emailTemplate name of the message template.
     * @param epa users to receive the message.
     * @param arguments to be substituted into the message template.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     * @throws MessagingException passed through.
     */
    public void alertUsersOnTaskActivation(Context c, XmlWorkflowItem wfi,
            String emailTemplate, List<EPerson> epa, String... arguments)
            throws IOException, SQLException, MessagingException;

    /**
     * Executes a workflow action and returns the next.
     *
     * @param c current DSpace session.
     * @param user user attempting the action.
     * @param request the current request.
     * @param workflowItemId the workflow item on which to take the action.
     * @param workflow the workflow holding the item.
     * @param currentActionConfig the requested action.
     * @return the next action to be executed.
     * @throws SQLException passed through.
     * @throws AuthorizeException if the user may not take this action.
     * @throws IOException passed through.
     * @throws MessagingException unused.
     * @throws WorkflowException if the action could not be executed.
     */
    public WorkflowActionConfig doState(Context c, EPerson user, HttpServletRequest request, int workflowItemId,
                                        Workflow workflow, WorkflowActionConfig currentActionConfig)
        throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowException;

    /**
     * Select the next action based on the outcome of a current action.
     *
     * @param c session context.
     * @param user current user.
     * @param workflow item is in this workflow.
     * @param currentStep workflow step being executed.
     * @param currentActionConfig describes the current step's action.
     * @param currentOutcome the result of executing the current step (accept/reject/etc).
     * @param wfi the Item being processed through workflow.
     * @param enteredNewStep is the Item advancing to a new workflow step?
     * @return the next step's action.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws WorkflowException if the current step's outcome is unrecognized.
     */
    public WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep,
                                               WorkflowActionConfig currentActionConfig, ActionResult currentOutcome,
                                               XmlWorkflowItem wfi, boolean enteredNewStep)
        throws IOException, AuthorizeException, SQLException, WorkflowException;

    /**
     * Deletes all tasks from a WorkflowItem.
     *
     * @param context current DSpace session
     * @param wi      the workflow item for which we are to delete the tasks
     * @throws SQLException       passed through.
     * @throws AuthorizeException passed through.
     */
    public void deleteAllTasks(Context context, XmlWorkflowItem wi) throws SQLException, AuthorizeException;

    /**
     * Deletes all pooled tasks from a WorkflowItem.
     * @param c  current DSpace session.
     * @param wi the workflow item from which we are to delete the tasks.
     * @throws SQLException       passed through.
     * @throws AuthorizeException passed through.
     */
    public void deleteAllPooledTasks(Context c, XmlWorkflowItem wi) throws SQLException, AuthorizeException;

    /**
     * Deletes a pooled (uncompleted) task from the task pool of a step.
     * @param context current DSpace session.
     * @param wi      the workflow item associated with the task.
     * @param task    the task to be removed.
     * @throws SQLException       passed through.
     * @throws AuthorizeException passed through.
     */
    public void deletePooledTask(Context context, XmlWorkflowItem wi, PoolTask task)
        throws SQLException, AuthorizeException;

    /**
     * Deletes a completed task of a step.
     * @param c    current DSpace session.
     * @param wi   the workflow item associated with the task.
     * @param task the task to be removed.
     * @throws SQLException       passed through.
     * @throws AuthorizeException passed through.
     */
    public void deleteClaimedTask(Context c, XmlWorkflowItem wi, ClaimedTask task)
        throws SQLException, AuthorizeException;

    /**
     * Create the task pool for a given item and workflow step.
     *
     * @param context
     * @param wi Create tasks for this item.
     * @param assignees Role members for this step.
     * @param step Create tasks for this step.
     * @param action
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void createPoolTasks(Context context, XmlWorkflowItem wi, RoleMembers assignees, Step step,
                                WorkflowActionConfig action)
        throws SQLException, AuthorizeException;

    /**
     * Create a claim on a task action for a given EPerson.
     *
     * @param context current DSpace session.
     * @param wi Claim tasks of this item.
     * @param step Claim tasks from this step.
     * @param action the action being claimed.
     * @param e Claimant.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void createOwnedTask(Context context, XmlWorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e)
        throws SQLException, AuthorizeException;

    /**
     * Grant a user full powers over an Item.
     *
     * @param context current DSpace session.
     * @param item grant powers over this item.
     * @param epa user to whom powers are granted.
     * @param actionType workflow, submission, etc.
     * @throws AuthorizeException passed through.
     * @throws SQLException passed through.
     */
    public void grantUserAllItemPolicies(Context context, Item item, EPerson epa, String actionType)
        throws AuthorizeException, SQLException;

    public void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException;

    /**
     * Send email to interested parties when curation tasks run.
     *
     * @param c session context.
     * @param wi the item being curated.
     * @param ePeople the interested parties.
     * @param taskName the task that has been run.
     * @param action the action indicated by the task (reject, approve, etc.)
     * @param message anything the code wants to say about the task.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     */
    public void notifyOfCuration(Context c, XmlWorkflowItem wi,
            List<EPerson> ePeople, String taskName, String action, String message)
            throws SQLException, IOException;

    /**
     * Get a description of an EPerson.
     *
     * @param ePerson the EPerson to be described.
     * @return the EPerson's full name and email address.
     */
    public String getEPersonName(EPerson ePerson);
}
