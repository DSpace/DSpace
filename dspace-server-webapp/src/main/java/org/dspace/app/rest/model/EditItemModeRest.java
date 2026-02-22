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

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSubmissionDefinition() {
        return submissionDefinition;
    }

    public void setSubmissionDefinition(String submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
