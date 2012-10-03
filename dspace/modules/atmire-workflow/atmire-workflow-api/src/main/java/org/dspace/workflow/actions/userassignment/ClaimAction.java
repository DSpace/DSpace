package org.dspace.workflow.actions.userassignment;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.*;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.actions.ActionResult;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:37:55
 * To change this template use File | Settings | File Templates.
 */
public class ClaimAction extends UserSelectionAction {
    @Override
    public void activate(Context context, WorkflowItem wfItem) throws SQLException, IOException, AuthorizeException {
        Step owningStep = getParent().getStep();

        EPerson[] allMembers = getParent().getStep().getRole().getMembers(context, wfItem);
        // Create pooled tasks for each member of our group
        if(allMembers != null && allMembers.length > 0){
            WorkflowManager.createPoolTasks(context, wfItem, allMembers, owningStep, getParent());
            alertUsersOnActivation(context, wfItem, allMembers);
        }
        else
            log.info(LogManager.getHeader(context, "warning while activating claim action", "No group was found for the following roleid: " + getParent().getStep().getRole().getId()));


    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(request.getParameter("submit_take_task") != null){
            //Add a claimed user to our task
            WorkflowRequirementsManager.addClaimedUser(c, wfi, step, c.getCurrentUser());

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }else{
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
    }

    @Override
    public void alertUsersOnActivation(Context c, WorkflowItem wfi, EPerson[] members) throws IOException, SQLException {
        Email mail = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "submit_task"));
        mail.addArgument(wfi.getItem().getName());
        mail.addArgument(wfi.getCollection().getName());
        mail.addArgument(wfi.getSubmitter().getFullName());
        //TODO: message
        mail.addArgument("New task available.");
        mail.addArgument(WorkflowManager.getMyDSpaceLink());

        try {
            WorkflowUtils.emailRecipients(members, mail);
        } catch (MessagingException e) {
            log.info(LogManager.getHeader(c, "error emailing user(s) for claimed task", "step: " + getParent().getStep().getId() + " workflowitem: " + wfi.getID()));
        }
    }

    public void regenerateTasks(Context c, WorkflowItem wfi, List<Integer> userToIgnore) throws SQLException, AuthorizeException {
        //Retrieve the workflow for the current workflowitem
        Step owningStep = getParent().getStep();
        EPerson[] roleMembers = getParent().getStep().getRole().getMembers(c, wfi);

        if(roleMembers != null && roleMembers.length>0){
            //Create a list out all the users we are to pool a task for
            List<EPerson> toPoolUsers = new ArrayList<EPerson>();
            for (EPerson user : roleMembers) {
                if (!userToIgnore.contains(user.getID()))
                    toPoolUsers.add(user);
            }
            //Create task for the users left
            WorkflowManager.createPoolTasks(c, wfi, toPoolUsers.toArray(new EPerson[toPoolUsers.size()]), owningStep, getParent());

        }
        else
            log.info(LogManager.getHeader(c, "warning while activating claim action", "No group was found for the following roleid: " + getParent().getStep().getId()));

    }

    @Override
    public boolean isFinished(WorkflowItem wfi) {
        return false;
    }

    public boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        //A user claim action always needs to have a UI, since somebody needs to be able to claim it
        if(hasUI){
            Step step = getParent().getStep();
            //First of all check if our step has a role
            Role role = step.getRole();
            if(role != null){
                //We have a role, check if we have a group to with that role
                EPerson[] allMembers = role.getMembers(context, wfi);
                //TODO: what about the requirements !, should we throw a requirementsexception if we do not have the required members in our group
                if(allMembers == null || allMembers.length == 0 && step.getRequiredUsers() <= allMembers.length){
                    //We don't have any members or our group is non existing
                    //and thus the step is not valid
                    return false;
                } else {
                    return true;
                }
            } else {
                // We don't have a role and do have a UI so throw a workflow exception
                throw new WorkflowConfigurationException("The next step is invalid, since it doesn't have a valid role");
            }
        }else
            return true;

    }

}
