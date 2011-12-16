package org.dspace.workflow;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 12-aug-2010
 * Time: 11:00:40
 */
public class WorkflowRequirementsManager {

    public static final String WORKFLOW_SCHEMA = "workflow";


    /**
     * Adds a claimed user in the metadata
     * if enough users have claimed this task (claimed or finished) to meet the required number
     * the pooled tasks will be deleted
     * @param c the dspace context
     * @param wfi the workflow item
     * @param step the step for which we are accepting
     * @param user the current user
     * @throws SQLException ...
     * @throws AuthorizeException ...
     * @throws IOException ...
     */
    public static void addClaimedUser(Context c, WorkflowItem wfi, Step step, EPerson user) throws SQLException, AuthorizeException, IOException {
        wfi.getItem().addMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", null, String.valueOf(user.getID()));

        int claimedUsers = getNumberOfInProgressUsers(wfi);
        int finishedUsers = getNumberOfFinishedUsers(wfi);

        int totalUsers = claimedUsers + finishedUsers;

        //Make sure we delete the pooled task for our current user
        PoolTask task = PoolTask.findByWorkflowIdAndEPerson(c,wfi.getID(),user.getID());
        WorkflowManager.deletePooledTask(c, wfi, task);

        if(totalUsers == step.getRequiredUsers()){
            //If enough users have claimed/finished this step then remove the tasks
            WorkflowManager.deleteAllPooledTasks(c, wfi);
        }
        wfi.update();
    }

    public static void removeClaimedUser(Context c, WorkflowItem wfi, EPerson user, String stepID) throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException {
        //Check if we had reached our max number @ this moment
        int totalUsers = getNumberOfInProgressUsers(wfi) + getNumberOfFinishedUsers(wfi);


        //Then remove the current user from the inProgressUsers
        DCValue[] claimedUsers = wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY);
        wfi.getItem().clearMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY);
        for (DCValue claimedUser : claimedUsers) {
            if(!claimedUser.value.equals(String.valueOf(user.getID())))
                wfi.getItem().addMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", null, claimedUser.value);
        }

        Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
        Step step = workflow.getStep(stepID);

//        WorkflowManager.deleteOwnedTask(c, user, wfi, step, step.getActionConfig());
        //We had reached our total user, but no anymore so recreate tasks for the user who don't have one
        if(totalUsers == step.getRequiredUsers()){

            //Create a list of the users we are to ignore
            List<Integer> toIgnoreUsers = new ArrayList<Integer>();
            DCValue[] finishedUsers = wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "finishedUsers", Item.ANY);
            for (DCValue finishedUser : finishedUsers)
                toIgnoreUsers.add(Integer.parseInt(finishedUser.value));

            // Also make sure that the inProgressUsers are ignored
            DCValue[] inProgressUsers = wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY);
            for (DCValue inProgressUser : inProgressUsers)
                toIgnoreUsers.add(Integer.parseInt(inProgressUser.value));

            //We have all our inProgress & finished users in a list now also add the onces who HAVE a pooled task (no need to create twice)
            List<PoolTask> pooledTasks = PoolTask.find(c, wfi);
            for (PoolTask poolTask : pooledTasks)
                toIgnoreUsers.add(poolTask.getEpersonID());

            step.getUserSelectionMethod().getProcessingAction().regenerateTasks(c, wfi, toIgnoreUsers);

        }
    }

    /**
     * Adds a finished user in the metadata
     * this method will also remove the user from the inprogress metadata
     * @param c the dspace context
     * @param wfi the workflow item
     * @param user the current user
     * @throws AuthorizeException ...
     * @throws SQLException ...
     */
    public static void addFinishedUser(Context c, WorkflowItem wfi, EPerson user) throws AuthorizeException, SQLException {
        int userId;
        if(user == null)
            userId = -1;
        else
            userId = user.getID();

        //First thing we do is remove the current user from the inProgressUsers
        DCValue[] claimedUsers = wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY);
        wfi.getItem().clearMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY);
        for (DCValue claimedUser : claimedUsers) {
            if(!claimedUser.value.equals(String.valueOf(userId)))
                wfi.getItem().addMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", null, claimedUser.value);
        }

        //Our current user has been removed from the inProgressUsers so add it to the finished onces
        //Should no user be found just add -1
        wfi.getItem().addMetadata(WORKFLOW_SCHEMA, "step", "finishedUsers", null, String.valueOf(userId));
        wfi.getItem().update();
    }

    public static int getNumberOfInProgressUsers(WorkflowItem wfi){
        return wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "inProgressUsers", Item.ANY).length;
    }

    public static int getNumberOfFinishedUsers(WorkflowItem wfi){
        return wfi.getItem().getMetadata(WORKFLOW_SCHEMA, "step", "finishedUsers", Item.ANY).length;
    }


    public static void clearStepMetadata(WorkflowItem wfi) throws AuthorizeException, SQLException {
        wfi.getItem().clearMetadata(WORKFLOW_SCHEMA, "step", Item.ANY, Item.ANY);
        wfi.getItem().update();
    }
}
