/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.*;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * A user selection action that assigns the original submitter
 * to the workflowitem
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AssignOriginalSubmitterAction extends UserSelectionAction{

    @Autowired(required = true)
    protected WorkflowRequirementsService workflowRequirementsService;

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return false;
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers) throws SQLException {

    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        return wfi.getSubmitter() != null;
    }

    @Override
    public boolean usesTaskPool() {
        return false;
    }

    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException, IOException {

    }

    @Override
    public void alertUsersOnActivation(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers) throws IOException, SQLException {
        try{
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.alertUsersOnTaskActivation(c, wfi, "submit_task", Arrays.asList(wfi.getSubmitter()),
                    //The arguments
                    wfi.getItem().getName(),
                    wfi.getCollection().getName(),
                    wfi.getSubmitter().getFullName(),
                    //TODO: message
                    "New task available.",
                    xmlWorkflowService.getMyDSpaceLink()
            );
        } catch (MessagingException e) {
            log.info(LogManager.getHeader(c, "error emailing user(s) for claimed task", "step: " + getParent().getStep().getId() + " workflowitem: " + wfi.getID()));
        }
    }


    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        EPerson submitter = wfi.getSubmitter();
        WorkflowActionConfig nextAction = getParent().getStep().getNextAction(this.getParent());
        //Retrieve the action which has a user interface
        while(nextAction != null && !nextAction.requiresUI()){
            nextAction = nextAction.getStep().getNextAction(nextAction);
        }
        if(nextAction == null)
        {
            //Should never occur, but just in case
            log.error("Could not find next action for step with id: " + step.getId() + " to assign a submitter to. Aborting the action.");
            throw new IllegalStateException();
        }

        createTaskForEPerson(c, wfi, step, nextAction, submitter);

        //It is important that we return to the submission page since we will continue our actions with the submitter
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

/**
     * Create a claimed task for the user IF this user doesn't have a claimed action for this workflow item
     * @param c the dspace context
     * @param wfi the workflow item
     * @param step  the current step
     * @param actionConfig the action
     * @param user the user to create the action for
     * @throws SQLException ...
     * @throws AuthorizeException ...
     * @throws IOException ...
     */
    protected void createTaskForEPerson(Context c, XmlWorkflowItem wfi, Step step, WorkflowActionConfig actionConfig, EPerson user) throws SQLException, AuthorizeException, IOException {
        if(claimedTaskService.find(c, wfi, step.getId(), actionConfig.getId()) != null){
            workflowRequirementsService.addClaimedUser(c, wfi, step, c.getCurrentUser());
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().createOwnedTask(c, wfi, step, actionConfig, user);
        }
    }

}
