/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * WorkflowItem table pointing to it.
 *
 * Once the item has completed the workflow it will be archived
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface XmlWorkflowService extends WorkflowService<XmlWorkflowItem> {

    public void alertUsersOnTaskActivation(Context c, XmlWorkflowItem wfi, String emailTemplate, List<EPerson> epa, String ...arguments) throws IOException, SQLException, MessagingException;

    public WorkflowActionConfig doState(Context c, EPerson user, HttpServletRequest request, int workflowItemId, Workflow workflow, WorkflowActionConfig currentActionConfig) throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowException;

    public WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep, WorkflowActionConfig currentActionConfig, ActionResult currentOutcome, XmlWorkflowItem wfi, boolean enteredNewStep) throws IOException, AuthorizeException, SQLException, WorkflowException;

    public void deleteAllTasks(Context context, XmlWorkflowItem wi) throws SQLException, AuthorizeException;

    public void deleteAllPooledTasks(Context c, XmlWorkflowItem wi) throws SQLException, AuthorizeException;

    public void deletePooledTask(Context context, XmlWorkflowItem wi, PoolTask task) throws SQLException, AuthorizeException;

    public void deleteClaimedTask(Context c, XmlWorkflowItem wi, ClaimedTask task) throws SQLException, AuthorizeException;

    public void createPoolTasks(Context context, XmlWorkflowItem wi, RoleMembers assignees, Step step, WorkflowActionConfig action)
            throws SQLException, AuthorizeException;

    public void createOwnedTask(Context context, XmlWorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e) throws SQLException, AuthorizeException;

    public void grantUserAllItemPolicies(Context context, Item item, EPerson epa) throws AuthorizeException, SQLException;

    public void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException;

    public String getEPersonName(EPerson ePerson);
}
