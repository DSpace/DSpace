/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The rest resource used for workflow steps
 *
 * @author Maria Verdonck (Atmire) on 10/01/2020
 */
@LinksRest(links = {
    @LinkRest(
        name = WorkflowStepRest.ACTIONS,
        method = "getActions"
    ),
})
public class WorkflowStepRest extends BaseObjectRest {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowstep";
    public static final String NAME_PLURAL = "workflowsteps";

    public static final String ACTIONS = "workflowactions";

    private List<WorkflowActionRest> workflowactions;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @JsonIgnore
    public List<WorkflowActionRest> getWorkflowactions() {
        return workflowactions;
    }

    public void setWorkflowactions(List<WorkflowActionRest> actions) {
        this.workflowactions = actions;
    }
}
