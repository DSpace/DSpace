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
 * The rest resource used for workflow definitions
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@LinksRest(links = {
    @LinkRest(
        name = WorkflowDefinitionRest.COLLECTIONS_MAPPED_TO,
        method = "getCollections"
    ),
    @LinkRest(
        name = WorkflowDefinitionRest.STEPS,
        method = "getSteps"
    )
})
public class WorkflowDefinitionRest extends BaseObjectRest<String> {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowdefinition";
    public static final String NAME_PLURAL = "workflowdefinitions";

    public static final String COLLECTIONS_MAPPED_TO = "collections";
    public static final String STEPS = "steps";

    private String name;
    private boolean isDefault;
    private List<WorkflowStepRest> steps;

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

    @Override
    @JsonIgnore
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @JsonIgnore
    public List<WorkflowStepRest> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepRest> steps) {
        this.steps = steps;
    }
}
