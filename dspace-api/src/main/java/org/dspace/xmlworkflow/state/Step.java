/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.UserSelectionActionConfig;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * A class that contains all the data of an xlworkflow step
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Step implements BeanNameAware {

    @Autowired
    protected InProgressUserService inProgressUserService;

    private UserSelectionActionConfig userSelectionMethod;
    private List<WorkflowActionConfig> actions;
    private Map<Integer, Step> outcomes = new HashMap<>();
    private String id;
    private Role role;
    private Workflow workflow;
    private int requiredUsers = 1;

    /**
     * Get an WorkflowActionConfiguration object for the provided action identifier
     * @param actionID the action id for which we want our action config
     * @return The corresponding WorkflowActionConfiguration
     * @throws WorkflowConfigurationException occurs if the provided action isn't part of the step
     */
    public WorkflowActionConfig getActionConfig(String actionID) throws WorkflowConfigurationException {
        // First check the userSelectionMethod as this is not a regular "action"
        if (userSelectionMethod != null && StringUtils.equals(userSelectionMethod.getId(), actionID)) {
            return userSelectionMethod;
        }
        for (WorkflowActionConfig actionConfig : actions) {
            if (StringUtils.equals(actionConfig.getId(), actionID)) {
                return actionConfig;
            }
        }
        throw new WorkflowConfigurationException("Action configuration not found for: " + actionID);
    }

    /**
     * Boolean that returns whether or not the actions in this step have a ui
     *
     * @return a boolean
     */
    public boolean hasUI() {
        for (WorkflowActionConfig actionConfig : actions) {
            if (actionConfig.requiresUI()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the next step based on out the outcome
     * @param outcome the outcome of the previous step
     * @return the next stepp or NULL if there is no step configured for this outcome
     */
    public Step getNextStep(int outcome) {
        return outcomes.get(outcome);
    }


    public boolean isValidStep(Context context, XmlWorkflowItem wfi)
        throws WorkflowConfigurationException, SQLException {
        //Check if our next step has a UI, if not then the step is valid, no need for a group
        return !(getUserSelectionMethod() == null || getUserSelectionMethod()
            .getProcessingAction() == null) && getUserSelectionMethod().getProcessingAction()
                                                                       .isValidUserSelection(context, wfi, hasUI());

    }

    public UserSelectionActionConfig getUserSelectionMethod() {
        return userSelectionMethod;
    }

    public WorkflowActionConfig getNextAction(WorkflowActionConfig currentAction) {
        int index = actions.indexOf(currentAction);
        if (index < actions.size() - 1) {
            return actions.get(index + 1);
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
     *
     * @param c   The relevant DSpace Context.
     * @param wfi the workflow item to check
     * @return if enough users have finished this task
     * @throws SQLException An exception that provides information on a database access error or other errors.
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

    /**
     * Set the user selection configuration, this is required as every step requires one
     * @param userSelectionMethod the user selection method configuration
     */
    @Required
    public void setUserSelectionMethod(UserSelectionActionConfig userSelectionMethod) {
        this.userSelectionMethod = userSelectionMethod;
        userSelectionMethod.setStep(this);
    }

    /**
     * Set the outcomes as a map, if no outcomes are configured this step will be last step in the workflow
     * @param outcomes the map containing the outcomes.
     */
    public void setOutcomes(Map<Integer, Step> outcomes) {
        this.outcomes = outcomes;
    }


    /**
     * Get the processing actions for the step. Processing actions contain the logic required to execute the required
     * operations in each step.
     * @return the actions configured for this step
     */
    public List<WorkflowActionConfig> getActions() {
        return actions;
    }

    /**
     * Set the processing actions for the step. Processing actions contain the logic required to execute the required
     * operations in each step.
     * @param actions the list of actions
     */
    @Required
    public void setActions(List<WorkflowActionConfig> actions) {
        for (WorkflowActionConfig workflowActionConfig : actions) {
            workflowActionConfig.setStep(this);
        }
        this.actions = actions;
    }

    /**
     * Set the workflow this step belongs to
     * @param workflow the workflow configuration
     */
    protected void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * Store the name of the bean in the identifier
     * @param s the bean name
     */
    @Override
    public void setBeanName(String s) {
        id = s;
    }

    /**
     * Set the number of required users that need to execute this step before it is completed,
     * the default is a single user
     * @param requiredUsers the number of required users
     */
    public void setRequiredUsers(int requiredUsers) {
        this.requiredUsers = requiredUsers;
    }

    /**
     * Set the role of which users role should execute this step
     * @param role the role to be configured for this step
     */
    public void setRole(Role role) {
        this.role = role;
    }
}
