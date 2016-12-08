/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.actions.UserSelectionActionConfig;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.*;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that contains all the data of an xlworkflow step
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Step {


    protected InProgressUserService inProgressUserService = XmlWorkflowServiceFactory.getInstance().getInProgressUserService();
    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();


    private UserSelectionActionConfig userSelectionMethod;
    private HashMap<String, WorkflowActionConfig> actionConfigsMap;
    private List<String> actionConfigsList;
    private Map<Integer, String> outcomes;
    private String id;
    private Role role;
    private Workflow workflow;
    private int requiredUsers;

    public Step(String id, Workflow workflow, Role role, UserSelectionActionConfig userSelectionMethod, List<String> actionConfigsList, Map<Integer, String> outcomes, int requiredUsers) {
        this.actionConfigsMap = new HashMap<>();
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
        if (actionConfigsMap.get(actionID)!=null) {
            return actionConfigsMap.get(actionID);
        } else {
            WorkflowActionConfig action = xmlWorkflowFactory.createWorkflowActionConfig(actionID);
            action.setStep(this);
            actionConfigsMap.put(actionID, action);
            return action;
        }
    }

    /**
     * Boolean that returns whether or not the actions in this step have a ui
     * @return a boolean
     */
    public boolean hasUI() {
        for (String actionConfigId : actionConfigsList) {
            WorkflowActionConfig actionConfig = getActionConfig(actionConfigId);
            if (actionConfig.requiresUI()) {
                return true;
            }
        }
        return false;
    }

    public String getNextStepID(int outcome) throws WorkflowException, IOException, WorkflowConfigurationException, SQLException {
        return outcomes.get(outcome);
    }


    public boolean isValidStep(Context context, XmlWorkflowItem wfi) throws WorkflowConfigurationException, SQLException {
        //Check if our next step has a UI, if not then the step is valid, no need for a group
        return !(getUserSelectionMethod() == null || getUserSelectionMethod().getProcessingAction() == null) && getUserSelectionMethod().getProcessingAction().isValidUserSelection(context, wfi, hasUI());

    }

    public UserSelectionActionConfig getUserSelectionMethod() {
            return userSelectionMethod;
    }

    public WorkflowActionConfig getNextAction(WorkflowActionConfig currentAction) {
        int index = actionConfigsList.indexOf(currentAction.getId());
        if (index < actionConfigsList.size()-1) {
            return getActionConfig(actionConfigsList.get(index+1));
        } else {
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
     * @param c
     *     The relevant DSpace Context.
     * @param wfi
     *     the workflow item to check
     * @return if enough users have finished this task
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public boolean isFinished(Context c, XmlWorkflowItem wfi) throws SQLException {
        return inProgressUserService.getNumberOfFinishedUsers(c, wfi) == requiredUsers;
    }

    public int getRequiredUsers() {
        return requiredUsers;
    }

    public Role getRole() {
        return role;
    }

//    public boolean skipStep() {
//    }
}
