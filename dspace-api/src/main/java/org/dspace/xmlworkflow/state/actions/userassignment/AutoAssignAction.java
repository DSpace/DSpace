/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.*;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.state.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * A user selection action that will create pooled tasks
 * for all the user who have a workflowItemRole for this
 * workflow item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AutoAssignAction extends UserSelectionAction {

    private static final Logger log = Logger.getLogger(AutoAssignAction.class);

    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {
        
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        try {
            Role role = getParent().getStep().getRole();
            if(role != null){
                WorkflowActionConfig nextAction = getParent().getStep().getNextAction(this.getParent());
                //Retrieve the action which has a user interface
                while(nextAction != null && !nextAction.requiresUI()){
                    nextAction = nextAction.getStep().getNextAction(nextAction);
                }

                if(nextAction != null){
                    WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.find(c, wfi.getID(), role.getId());
                    for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
                        if(workflowItemRole.getEPerson() != null){
                            createTaskForEPerson(c, wfi, step, nextAction, workflowItemRole.getEPerson());
                        }else{
                            EPerson[] members = Group.allMembers(c, workflowItemRole.getGroup());
                            for (EPerson member : members) {
                                createTaskForEPerson(c, wfi, step, nextAction, member);
                            }
                        }
                        //Delete our workflow item role since the users have been assigned
                        workflowItemRole.delete();
                    }
                }else{
                    log.warn(LogManager.getHeader(c, "Error while executing auto assign action", "No valid next action. Workflow item:" + wfi.getID()));
                }
            }
        } catch (SQLException e) {
            log.error(LogManager.getHeader(c, "Error while executing auto assign action", "Workflow item: " + wfi.getID() + " step :" + getParent().getStep().getId()), e);
            throw e;
        } catch (AuthorizeException e) {
            log.error(LogManager.getHeader(c, "Error while executing auto assign action", "Workflow item: " + wfi.getID() + " step :" + getParent().getStep().getId()), e);
            throw e;
        } catch (IOException e) {
            log.error(LogManager.getHeader(c, "Error while executing auto assign action", "Workflow item: " + wfi.getID() + " step :" + getParent().getStep().getId()), e);
            throw e;
        }


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
    private void createTaskForEPerson(Context c, XmlWorkflowItem wfi, Step step, WorkflowActionConfig actionConfig, EPerson user) throws SQLException, AuthorizeException, IOException {
        if(ClaimedTask.find(c, wfi.getID(), step.getId(), actionConfig.getId()) != null){
            WorkflowRequirementsManager.addClaimedUser(c, wfi, step, c.getCurrentUser());
            XmlWorkflowManager.createOwnedTask(c, wfi, step, actionConfig,user);
        }
    }

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return true;
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi,  RoleMembers roleMembers) throws SQLException {

    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        //This is an automatic assign action, it can never have a user interface
        Role role = getParent().getStep().getRole();
        if(role != null){
            WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.find(context, wfi.getID(), role.getId());
            if(workflowItemRoles.length == 0){
                throw new WorkflowConfigurationException("The next step is invalid, since it doesn't have any individual item roles");
            }
            return true;
        }else{
            throw new WorkflowConfigurationException("The next step is invalid, since it doesn't have a valid role");
        }
    }

    @Override
    public boolean usesTaskPool() {
        return false;
    }
}
