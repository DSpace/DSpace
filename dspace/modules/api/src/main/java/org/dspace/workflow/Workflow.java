package org.dspace.workflow;

import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 3-aug-2010
 * Time: 14:11:06
 * To change this template use File | Settings | File Templates.
 */
public class Workflow {

    private String id;
    private Step firstStep;
    private HashMap<String, Step> steps;
    private HashMap<String, Role> roles;
    private List<String> allStepIdentifiers = null;


    public Workflow(String workflowID, HashMap<String, Role> roles) {
        this.id = workflowID;
        this.roles = roles;
        this.steps = new HashMap<String, Step>();
    }

    public Step getFirstStep() {
        return firstStep;
    }

    public String getID(){
        return id;
    }

    /*
     * Return a step with a given id
     */
    public Step getStep(String stepID) throws WorkflowConfigurationException, IOException {
        if(steps.get(id)!=null){
            return steps.get(id);
        }else{
            Step step = WorkflowFactory.createStep(this, stepID);
            if(step== null){
                throw new WorkflowConfigurationException("Step definition not found for: "+stepID);
            }
            steps.put(stepID, step);
            return step;
        }
    }

    public Step getNextStep(Context context, WorkflowItem wfi, Step currentStep, int outcome) throws IOException, WorkflowConfigurationException, WorkflowException, SQLException {
        String nextStepID = currentStep.getNextStepID(outcome);
        if(nextStepID != null){
            Step nextStep = getStep(nextStepID);
            if(nextStep == null)
                throw new WorkflowException("Error while processing outcome, the following action was undefined: " + nextStepID);
            if(nextStep.isValidStep(context, wfi)){
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

    public List<String> getStepIdentifiers() throws WorkflowConfigurationException {
        if(allStepIdentifiers == null){
            allStepIdentifiers = WorkflowFactory.getAllStepIds(this);
        }
        return allStepIdentifiers;
    }
}
