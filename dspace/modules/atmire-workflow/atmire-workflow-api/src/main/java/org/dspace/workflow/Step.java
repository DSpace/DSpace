package org.dspace.workflow;

import org.dspace.core.Context;
import org.dspace.workflow.actions.UserSelectionActionConfig;
import org.dspace.workflow.actions.WorkflowActionConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:39:45
 * To change this template use File | Settings | File Templates.
 */
public class Step {



    private UserSelectionActionConfig userSelectionMethod;
    private HashMap<String, WorkflowActionConfig> actionConfigsMap;
    private List<String> actionConfigsList;
    private List<String> outcomes;
    private String id;
    private Role role;
    private Workflow workflow;
    //TODO:
    private String name = this.toString();
    private int requiredUsers;

    public Step(String id, Workflow workflow, Role role, UserSelectionActionConfig userSelectionMethod, List<String> actionConfigsList, List<String> outcomes, int requiredUsers){
        this.actionConfigsMap = new HashMap<String, WorkflowActionConfig>();
        this.outcomes = outcomes;
        this.userSelectionMethod = userSelectionMethod;
        this.role = role;
        this.actionConfigsList = actionConfigsList;
        this.id = id;
        userSelectionMethod.setStep(this);
        this.requiredUsers = requiredUsers;
        this.workflow = workflow;

    }

    public WorkflowActionConfig getActionConfig(String actionID) {
        if(actionConfigsMap.get(actionID)!=null){
            return actionConfigsMap.get(actionID);
        }else{
            WorkflowActionConfig action = WorkflowFactory.createWorkflowActionConfig(actionID);
            action.setStep(this);
            actionConfigsMap.put(actionID, action);
            return action;
        }
    }

    /**
     * Boolean that returns whether or not the actions in this step have a ui
     * @return a boolean
     */
    public boolean hasUI(){
        for (String actionConfigId : actionConfigsList) {
            WorkflowActionConfig actionConfig = getActionConfig(actionConfigId);
            if (actionConfig.hasUserInterface()) {
                return true;
            }
        }
        return false;
    }

    public String getNextStepID(int outcome) throws WorkflowException, IOException, WorkflowConfigurationException, SQLException {
        return outcomes.get(outcome);
    }


    public boolean isValidStep(Context context, WorkflowItem wfi) throws WorkflowConfigurationException, SQLException {
        //Check if our next step has a UI, if not then the step is valid, no need for a group
        if(getUserSelectionMethod() == null || getUserSelectionMethod().getProcessingAction() == null){
            return false;
        }else{
            return getUserSelectionMethod().getProcessingAction().isValidUserSelection(context, wfi, hasUI());
        }

    }

    public UserSelectionActionConfig getUserSelectionMethod() {
            return userSelectionMethod;
    }

    public WorkflowActionConfig getNextAction(WorkflowActionConfig currentAction) {
        int index = actionConfigsList.indexOf(currentAction.getId());
        if(index < actionConfigsList.size()-1){
            return getActionConfig(actionConfigsList.get(index+1));
        }else{
            return null;  
        }
    }

    public String getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }


    /**
     * Check if enough users have finished this step for it to continue
     * @param wfi the workflow item to check
     * @return if enough users have finished this task
     */
    public boolean isFinished(WorkflowItem wfi){
        return WorkflowRequirementsManager.getNumberOfFinishedUsers(wfi) == requiredUsers;
    }

    public int getRequiredUsers(){
        return requiredUsers;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

//    public boolean skipStep(){
//    }
}
