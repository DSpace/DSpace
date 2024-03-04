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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class is the REST representation of the CCLicense model object and acts as a data object
 * for the SubmissionCCLicenseResource class.
 * Refer to {@link org.dspace.license.CCLicense} for explanation of the properties
 */
public class SubmissionCCLicenseRest extends BaseObjectRest<String> {
    public static final String NAME = "submissioncclicense";
    public static final String PLURAL_NAME = "submissioncclicenses";

    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    private String id;

    private String name;

    private List<SubmissionCCLicenseFieldRest> fields;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<SubmissionCCLicenseFieldRest> getFields() {
        return fields;
    }

    public void setFields(final List<SubmissionCCLicenseFieldRest> fields) {
        this.fields = fields;
    }

    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
