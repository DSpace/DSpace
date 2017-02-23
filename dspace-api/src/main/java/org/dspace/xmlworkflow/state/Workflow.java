/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import org.dspace.core.Context;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Class that contains all the steps and roles involved in a certain
 * configured workflow
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Workflow {

    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private String id;
    private Step firstStep;
    private HashMap<String, Step> steps;
    private LinkedHashMap<String, Role> roles;


    public Workflow(String workflowID, LinkedHashMap<String, Role> roles) {
        this.id = workflowID;
        this.roles = roles;
        this.steps = new HashMap<String, Step>();
    }

    public Step getFirstStep() {
        return firstStep;
    }

    public String getID() {
        return id;
    }

    /*
     * Return a step with a given id
     */
    public Step getStep(String stepID) throws WorkflowConfigurationException, IOException {
        if (steps.get(id)!=null) {
            return steps.get(id);
        } else {
            Step step = xmlWorkflowFactory.createStep(this, stepID);
            if (step== null){
                throw new WorkflowConfigurationException("Step definition not found for: "+stepID);
            }
            steps.put(stepID, step);
            return step;
        }
    }

    public Step getNextStep(Context context, XmlWorkflowItem wfi, Step currentStep, int outcome) throws IOException, WorkflowConfigurationException, WorkflowException, SQLException {
        String nextStepID = currentStep.getNextStepID(outcome);
        if (nextStepID != null) {
            Step nextStep = getStep(nextStepID);
            if (nextStep == null)
                throw new WorkflowException("Error while processing outcome, the following action was undefined: " + nextStepID);
            if (nextStep.isValidStep(context, wfi)) {
                return nextStep;
            } else {
                return getNextStep(context, wfi, nextStep, 0);
            }

        }else{
            //No next step, archive it
            return null;
        }

    }

    public void setFirstStep(Step firstStep) {
        this.firstStep = firstStep;
    }

    public HashMap<String, Role> getRoles() {
        return roles;
    }
}
