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
 * The rest resource used for workflow definitions
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
public class WorkflowDefinitionRest extends BaseObjectRest<String> {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowdefinition";
    public static final String NAME_PLURAL = "workflowdefinitions";
    public static final String TYPE = "workflow-definition";

    private String name;
    private boolean isDefault;

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
        return TYPE;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return id;
    }

    /**
     * Generic getter for the name
     *
     * @return the name value of this WorkflowDefinitionRest
     */
    public String getName() {
        return name;
    }

    /**
     * Generic setter for the name
     *
     * @param name The name to be set on this WorkflowDefinitionRest
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generic getter for the isDefault
     *
     * @return the isDefault value of this WorkflowDefinitionRest
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Generic setter for the isDefault
     *
     * @param isDefault The isDefault to be set on this WorkflowDefinitionRest
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
