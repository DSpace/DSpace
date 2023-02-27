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
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing class for an action where x number of users
 * have to accept a task from a designated pool
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ClaimAction extends UserSelectionAction {
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void activate(Context context, XmlWorkflowItem wfItem) throws SQLException, IOException, AuthorizeException {
        Step owningStep = getParent().getStep();

        RoleMembers allroleMembers = getParent().getStep().getRole().getMembers(context, wfItem);
        // Create pooled tasks for each member of our group
        if (allroleMembers != null && (allroleMembers.getGroups().size() > 0 || allroleMembers.getEPersons()
                                                                                              .size() > 0)) {
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                     .createPoolTasks(context, wfItem, allroleMembers, owningStep, getParent());
            alertUsersOnActivation(context, wfItem, allroleMembers);
        } else {
            log.info(LogHelper.getHeader(context, "warning while activating claim action",
                                          "No group or person was found for the following roleid: " + getParent()
                                              .getStep().getRole().getId()));
        }


    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        // accept task, or accepting multiple tasks
        XmlWorkflowServiceFactory.getInstance().getWorkflowRequirementsService().addClaimedUser(c, wfi, step,
                c.getCurrentUser());
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

    @Override
    public void alertUsersOnActivation(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers)
        throws IOException, SQLException {
        try {
            EPerson ep = wfi.getSubmitter();
            String submitterName = null;
            if (ep != null) {
                submitterName = ep.getFullName();
            }
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.alertUsersOnTaskActivation(c, wfi, "submit_task", roleMembers.getAllUniqueMembers(c),
                    //The arguments
                    wfi.getItem().getName(),
                    wfi.getCollection().getName(),
                    submitterName,
                    //TODO: message
                    "New task available.",
                    xmlWorkflowService.getMyDSpaceLink()
            );
        } catch (MessagingException e) {
            log.info(LogHelper.getHeader(c, "error emailing user(s) for claimed task",
                    "step: " + getParent().getStep().getId() + " workflowitem: " + wfi.getID()));
        }
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers)
        throws SQLException, AuthorizeException, IOException {
        if (roleMembers != null && (roleMembers.getEPersons().size() > 0 || roleMembers.getGroups().size() > 0)) {
            //Create task for the users left
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                     .createPoolTasks(c, wfi, roleMembers, getParent().getStep(), getParent());
            if (configurationService.getBooleanProperty("workflow.notify.returned.tasks", true)) {
                alertUsersOnActivation(c, wfi, roleMembers);
            }

        } else {
            log.info(LogHelper.getHeader(c, "warning while activating claim action",
                                          "No group or person was found for the following roleid: " + getParent()
                                              .getStep().getId()));
        }

    }

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return false;
    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI)
        throws WorkflowConfigurationException, SQLException {
        //A user claim action always needs to have a UI, since somebody needs to be able to claim it
        if (hasUI) {
            Step step = getParent().getStep();
            //First of all check if our step has a role
            Role role = step.getRole();
            if (role != null) {
                //We have a role, check if we have a group to with that role
                RoleMembers roleMembers = role.getMembers(context, wfi);

                ArrayList<EPerson> epersons = roleMembers.getAllUniqueMembers(context);
                if (epersons.isEmpty() || step.getRequiredUsers() > epersons.size()) {
                    log.warn(String.format("There must be at least %s ePerson(s) in the group",
                                           step.getRequiredUsers()));
                }
                return !(epersons.isEmpty() || step.getRequiredUsers() > epersons.size());
            } else {
                // We don't have a role and do have a UI so throw a workflow exception
                throw new WorkflowConfigurationException(
                    "The next step is invalid, since it doesn't have a valid role");
            }
        } else {
            return true;
        }

    }

    @Override
    public boolean usesTaskPool() {
        return true;
    }

}
