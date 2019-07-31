/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

/**
 * The Submission Form REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SubmissionFormRest extends BaseObjectRest<String> {
    public static final String NAME = "submissionform";
    public static final String NAME_LINK_ON_PANEL = RestAddressableModel.CONFIGURATION;
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    /** 
     * An unique name identifying the submission form
     */
    private String name;

    /**
     * The list of row in the submission form
     */
    private List<SubmissionFormRowRest> rows;

    @Override
    /**
     * The id of the submission form is its name
     */
    public String getId() {
        return name;
    }

    /**
     * Setter for {@link #name}
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for {@link #name}
     * 
     * @return {@link #name}
     */
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Getter for {@link #rows}
     * 
     * @return {@link #rows}
     */
    public List<SubmissionFormRowRest> getRows() {
        return rows;
    }

    /**
     * Setter for {@link #rows}
     * 
     */
    public void setRows(List<SubmissionFormRowRest> rows) {
        this.rows = rows;
    }
}
