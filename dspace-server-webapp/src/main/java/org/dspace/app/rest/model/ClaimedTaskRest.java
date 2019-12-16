/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;

/**
 * The ClaimedTask REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class ClaimedTaskRest extends BaseObjectRest<Integer> {
    public static final String NAME = "claimedtask";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    private String step;

    private String action;

    @JsonIgnore
    private EPersonRest owner;

    @JsonIgnore
    private WorkflowItemRest workflowitem;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * @see ClaimedTask#getStepID()
     * @return the step
     */
    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    /**
     * @see ClaimedTaskRest#getAction()
     * @return the action
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @see ClaimedTaskRest#getOwner()
     * @return the owner of the task
     */
    public EPersonRest getOwner() {
        return owner;
    }

    public void setOwner(EPersonRest owner) {
        this.owner = owner;
    }

    /**
     * 
     * @return the WorkflowItemRest that belong to this claimed task
     */
    public WorkflowItemRest getWorkflowitem() {
        return workflowitem;
    }

    public void setWorkflowitem(WorkflowItemRest workflowitem) {
        this.workflowitem = workflowitem;
    }
}
