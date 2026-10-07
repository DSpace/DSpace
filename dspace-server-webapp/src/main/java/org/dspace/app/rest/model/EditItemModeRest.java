/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The EditItemMode REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemModeRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -3615146164199721822L;
    public static final String NAME = "edititemmode";
    public static final String CATEGORY = RestAddressableModel.CORE;
    public static final String PLURAL_NAME = "edititemmodes";

    private String name;
    private String label;
    private String submissionDefinition;

    /**
     * Returns the type identifier for this REST resource.
     * This identifies the resource as an edit item mode in the REST API.
     *
     * @return the type name for edit item mode resources
     */
    @Override
    public String getType() {
        return NAME;
    }

    /**
     * Returns the plural form of the type identifier for this REST resource.
     * Used for collections of edit item mode resources in the REST API.
     *
     * @return the plural type name for edit item mode resources
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Gets the name of this edit item mode.
     * The name serves as a unique identifier for the mode within the system.
     *
     * @return the name of the edit item mode
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this edit item mode.
     * The name serves as a unique identifier for the mode within the system.
     *
     * @param name the name to set for this edit item mode
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the human-readable label for this edit item mode.
     * The label is typically displayed in the user interface to describe the mode.
     *
     * @return the display label of the edit item mode
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the human-readable label for this edit item mode.
     * The label is typically displayed in the user interface to describe the mode.
     *
     * @param label the display label to set for this edit item mode
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the submission definition associated with this edit item mode.
     * The submission definition determines the workflow and metadata requirements
     * for items edited in this mode.
     *
     * @return the name of the associated submission definition
     */
    public String getSubmissionDefinition() {
        return submissionDefinition;
    }

    /**
     * Sets the submission definition associated with this edit item mode.
     * The submission definition determines the workflow and metadata requirements
     * for items edited in this mode.
     *
     * @param submissionDefinition the name of the submission definition to associate with this mode
     */
    public void setSubmissionDefinition(String submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }

    /**
     * Returns the category this REST resource belongs to.
     * Edit item mode resources belong to the CORE category of the DSpace REST API.
     *
     * @return the category identifier for this resource type
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Returns the controller class responsible for handling REST operations on this resource.
     * Edit item mode resources are managed by the standard RestResourceController.
     *
     * @return the controller class for edit item mode resources
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
