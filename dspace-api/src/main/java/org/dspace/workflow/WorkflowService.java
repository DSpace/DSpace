/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;

/**
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * cwf_workflowitem table pointing to it.
 *
 * Once the item has completed the workflow it will be archived
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface WorkflowService {
    /**
     * startWorkflow() begins a workflow - in a single transaction do away with
     * the PersonalWorkspace entry and turn it into a WorkflowItem.
     *
     * @param context The relevant DSpace Context.
     * @param wsi     The WorkspaceItem to convert to a workflow item
     * @return The resulting workflow item
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *         to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws WorkflowException  if workflow error
     */
    public WorkflowItem start(Context context, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * startWithoutNotify() starts the workflow normally, but disables
     * notifications (useful for large imports,) for the first workflow step -
     * subsequent notifications happen normally
     *
     * @param c   The relevant DSpace Context.
     * @param wsi workspace item
     * @return the resulting workflow item.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *         to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws WorkflowException  if workflow error
     */
    public WorkflowItem startWithoutNotify(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * abort() aborts a workflow, completely deleting it (administrator do this)
     * (it will basically do a reject from any state - the item ends up back in
     * the user's PersonalWorkspace
     *
     * @param c  The relevant DSpace Context.
     * @param wi WorkflowItem to operate on
     * @param e  EPerson doing the operation
     * @return workspace item returned to workspace
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *         to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public WorkspaceItem abort(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, AuthorizeException, IOException;

    /**
     * Deletes workflow task item in correct order.
     *
     * @param c  The relevant DSpace Context.
     * @param wi The WorkflowItem that shall be deleted.
     * @param e  Admin that deletes this workflow task and item (for logging
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *         to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public void deleteWorkflowByWorkflowItem(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, AuthorizeException, IOException;

    public WorkspaceItem sendWorkflowItemBackSubmission(Context c, WorkflowItem workflowItem, EPerson e,
                                                        String provenance,
                                                        String rejection_message)
            throws SQLException, AuthorizeException, IOException;

    public String getMyDSpaceLink();

    public void deleteCollection(Context context, Collection collection)
            throws SQLException, IOException, AuthorizeException;

    public List<String> getEPersonDeleteConstraints(Context context, EPerson ePerson) throws SQLException;

    public Group getWorkflowRoleGroup(Context context, Collection collection, String roleName, Group roleGroup)
            throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException, WorkflowException;

    /**
     * This method will create the workflowRoleGroup for a collection and the given rolename
     * @param context       The relevant DSpace context
     * @param collection    The collection
     * @param roleName      The rolename
     * @return The created Group
     * @throws AuthorizeException If something goes wrong
     * @throws SQLException If something goes wrong
     * @throws IOException If something goes wrong
     * @throws WorkflowConfigurationException If something goes wrong
     */
    public Group createWorkflowRoleGroup(Context context, Collection collection, String roleName)
            throws AuthorizeException, SQLException, IOException, WorkflowConfigurationException;

    public List<String> getFlywayMigrationLocations();

    public void alertUsersOnTaskActivation(Context c, WorkflowItem wfi, String emailTemplate, List<EPerson> epa,
                                           String... arguments) throws IOException, SQLException, MessagingException;

    public WorkflowActionConfig doState(Context c, EPerson user, HttpServletRequest request, int workflowItemId,
                                        Workflow workflow, WorkflowActionConfig currentActionConfig)
            throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowException;

    public WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep,
                                               WorkflowActionConfig currentActionConfig, ActionResult currentOutcome,
                                               WorkflowItem wfi, boolean enteredNewStep)
            throws IOException, AuthorizeException, SQLException, WorkflowException;

    public void deleteAllTasks(Context context, WorkflowItem wi) throws SQLException, AuthorizeException;

    public void deleteAllPooledTasks(Context c, WorkflowItem wi) throws SQLException, AuthorizeException;

    public void deletePooledTask(Context context, WorkflowItem wi, PoolTask task)
            throws SQLException, AuthorizeException;

    public void deleteClaimedTask(Context c, WorkflowItem wi, ClaimedTask task)
            throws SQLException, AuthorizeException;

    public void createPoolTasks(Context context, WorkflowItem wi, RoleMembers assignees, Step step,
                                WorkflowActionConfig action)
            throws SQLException, AuthorizeException;

    public void createOwnedTask(Context context, WorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e)
            throws SQLException, AuthorizeException;

    public void grantUserAllItemPolicies(Context context, Item item, EPerson epa, String actionType)
            throws AuthorizeException, SQLException;

    public void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException;

    public String getEPersonName(EPerson ePerson);
}
