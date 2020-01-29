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

import org.dspace.core.Context;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

/**
 * Class that contains all the steps and roles involved in a certain
 * configured workflow
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Workflow implements BeanNameAware {

    private String id;
    private Step firstStep;
    private List<Step> steps;

    public Step getFirstStep() {
        return firstStep;
    }

    public String getID() {
        return id;
    }

    /*
     * Return a step with a given id
     */
    public Step getStep(String stepID) throws WorkflowConfigurationException {
        for (Step step : steps) {
            if (step.getId().equals(stepID)) {
                return step;
            }
        }
        throw new WorkflowConfigurationException("Step definition not found for: " + stepID);
    }

    public Step getNextStep(Context context, XmlWorkflowItem wfi, Step currentStep, int outcome)
        throws WorkflowConfigurationException, SQLException {
        Step nextStep = currentStep.getNextStep(outcome);
        if (nextStep != null) {
            if (nextStep.isValidStep(context, wfi)) {
                return nextStep;
            } else {
                return getNextStep(context, wfi, nextStep, ActionResult.OUTCOME_COMPLETE);
            }
        } else {
            //No next step, archive it
            return null;
        }
    }

    @Required
    public void setFirstStep(Step firstStep) {
        firstStep.setWorkflow(this);
        this.firstStep = firstStep;
    }

    /**
     * Get the steps that need to be executed in this workflow before the item is archived
     * @return the workflow steps
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * Set the steps that need to be executed in this workflow before the item is archived
     * @param steps the workflow steps
     */
    @Required
    public void setSteps(List<Step> steps) {
        for (Step step : steps) {
            step.setWorkflow(this);
        }
        this.steps = steps;
    }

    /**
     * Get the roles that are used in this workflow
     * @return a map containing the roles, the role name will the key, the role itself the value
     */
    public Map<String, Role> getRoles() {
        Map<String, Role> roles = new HashMap<>();
        for (Step step : steps) {
            if (step.getRole() != null) {
                roles.put(step.getRole().getName(), step.getRole());
            }
        }
        return roles;
    }

    @Override
    public void setBeanName(String s) {
        id = s;
    }
}
