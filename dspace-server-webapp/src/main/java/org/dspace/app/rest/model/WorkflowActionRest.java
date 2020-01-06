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

import java.util.List;

/**
 * The rest resource used for workflow actions
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
public class WorkflowActionRest extends BaseObjectRest<String> {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowaction";
    public static final String NAME_PLURAL = "workflowactions";

    private String name;
    private List<String> options;

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

    /**
     * Generic getter for the name
     *
     * @return the name value of thisWorkflowActionRest
     */
    public String getName() {
        return name;
    }

    /**
     * Generic getter for the options
     *
     * @return the options value of this WorkflowActionRest
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * Generic setter for the name
     *
     * @param name The name to be set on this WorkflowActionRest
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generic setter for the options
     *
     * @param options The options to be set on this WorkflowActionRest
     */
    public void setOptions(List<String> options) {
        this.options = options;
    }
}
