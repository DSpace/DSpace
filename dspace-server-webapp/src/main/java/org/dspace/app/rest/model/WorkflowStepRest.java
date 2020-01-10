/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.app.rest.RestResourceController;

import java.util.List;

/**
 * The rest resource used for workflow steps
 *
 * @author Maria Verdonck (Atmire) on 10/01/2020
 */
public class WorkflowStepRest extends BaseObjectRest {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowstep";
    public static final String NAME_PLURAL = "workflowsteps";

    private List<WorkflowActionRest> actions;

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

    @LinkRest(linkClass = WorkflowActionConfig.class)
    @JsonIgnore
    public List<WorkflowActionRest> getActions() {
        return actions;
    }

    public void setActions(List<WorkflowActionRest> actions) {
        this.actions = actions;
    }
}
