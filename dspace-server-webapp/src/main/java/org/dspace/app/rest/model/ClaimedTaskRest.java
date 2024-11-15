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

/**
 * The ClaimedTask REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(
        name = ClaimedTaskRest.STEP,
        method = "getStep"
    )
})
public class ClaimedTaskRest extends BaseObjectRest<Integer> {
    public static final String NAME = "claimedtask";
    public static final String PLURAL_NAME = "claimedtasks";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    public static final String STEP = "step";

    @JsonIgnore
    private WorkflowActionRest action;

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
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * @see ClaimedTaskRest#getAction()
     * @return the action
     */
    public WorkflowActionRest getAction() {
        return action;
    }

    public void setAction(WorkflowActionRest action) {
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
