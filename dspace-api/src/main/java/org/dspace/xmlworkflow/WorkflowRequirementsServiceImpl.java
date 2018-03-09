/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.*;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * A class that contains utililty methods related to the workflow
 * The adding/removing from claimed users and ensuring that
 * if multiple users have to perform these steps that a count is kept
 * so that no more then the allowed user count are allowed to perform their actions
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowRequirementsServiceImpl implements WorkflowRequirementsService {

    @Autowired(required = true)
    protected InProgressUserService inProgressUserService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected XmlWorkflowFactory workflowFactory;
    @Autowired(required = true)
    protected XmlWorkflowItemService xmlWorkflowItemService;
    @Autowired(required = true)
    protected XmlWorkflowService xmlWorkflowService;

    protected WorkflowRequirementsServiceImpl()
    {

    }

    @Override
    public void addClaimedUser(Context context, XmlWorkflowItem wfi, Step step, EPerson user) throws SQLException, AuthorizeException, IOException {

        //Make sure we delete the pooled task for our current user if the task is not a group pooltask
        PoolTask task = poolTaskService.findByWorkflowIdAndEPerson(context,wfi,user);
        if(task != null && task.getEperson() != null){
            xmlWorkflowService.deletePooledTask(context, wfi, task);
        }

        InProgressUser ipu = inProgressUserService.create(context);
        ipu.setWorkflowItem(wfi);
        ipu.setUser(user);
        ipu.setFinished(false);
        inProgressUserService.update(context, ipu);

        //Make sure the user has the necessary rights to update the item after the tasks is removed from the pool
        xmlWorkflowService.grantUserAllItemPolicies(context, wfi.getItem(), user);

        int totalUsers = inProgressUserService.getNumberOfInProgressUsers(context, wfi) + inProgressUserService.getNumberOfFinishedUsers(context, wfi);

        if(totalUsers == step.getRequiredUsers()){
            //If enough users have claimed/finished this step then remove the tasks
            xmlWorkflowService.deleteAllPooledTasks(context, wfi);
        }

        xmlWorkflowItemService.update(context, wfi);
    }

    @Override
    public void removeClaimedUser(Context context, XmlWorkflowItem wfi, EPerson user, String stepID) throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException {
        //Check if we had reached our max number @ this moment
        int totalUsers = inProgressUserService.getNumberOfInProgressUsers(context, wfi) + inProgressUserService.getNumberOfFinishedUsers(context, wfi);

        //Then remove the current user from the inProgressUsers
        inProgressUserService.delete(context, inProgressUserService.findByWorkflowItemAndEPerson(context, wfi, user));

        //Make sure the removed user has his custom rights removed
        xmlWorkflowService.removeUserItemPolicies(context, wfi.getItem(), user);

        Workflow workflow = workflowFactory.getWorkflow(wfi.getCollection());
        Step step = workflow.getStep(stepID);

//        WorkflowManager.deleteOwnedTask(c, user, wfi, step, step.getActionConfig());
        //We had reached our total user, so recreate tasks for the user who don't have one
        if(totalUsers == step.getRequiredUsers()){

            //Create a list of the users we are to ignore
            List<InProgressUser> toIgnore = inProgressUserService.findByWorkflowItem(context, wfi);

            //Remove the users to ignore
            RoleMembers roleMembers = step.getRole().getMembers(context, wfi);
            //Create a list out all the users we are to pool a task for
            for (InProgressUser ipu: toIgnore) {
                roleMembers.removeEperson(ipu.getUser());
            }
            step.getUserSelectionMethod().getProcessingAction().regenerateTasks(context, wfi, roleMembers);

        }else{
            //If the user previously had a personal PoolTask, this must be regenerated. Therefore we call the regeneration method
            //with only one EPerson
            RoleMembers role = step.getRole().getMembers(context, wfi);
            List<EPerson> epersons = role.getEPersons();
            for(EPerson eperson: epersons){
                if(eperson.getID().equals(user.getID())){
                    RoleMembers memberToRegenerateTasksFor = new RoleMembers();
                    memberToRegenerateTasksFor.addEPerson(user);
                    step.getUserSelectionMethod().getProcessingAction().regenerateTasks(context, wfi, memberToRegenerateTasksFor);
                    break;
                }
            }
        }
        //Update our item
        itemService.update(context, wfi.getItem());
    }

    @Override
    public void addFinishedUser(Context c, XmlWorkflowItem wfi, EPerson user) throws AuthorizeException, SQLException {
        InProgressUser ipu = inProgressUserService.findByWorkflowItemAndEPerson(c, wfi, user);
        ipu.setFinished(true);
        inProgressUserService.update(c, ipu);
    }


    @Override
    public void clearInProgressUsers(Context c, XmlWorkflowItem wfi) throws AuthorizeException, SQLException {
        Iterator<InProgressUser> ipus = inProgressUserService.findByWorkflowItem(c, wfi).iterator();
        while(ipus.hasNext())
        {
            InProgressUser ipu = ipus.next();
            ipus.remove();
            inProgressUserService.delete(c, ipu);
        }
    }

}
