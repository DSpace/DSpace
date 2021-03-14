/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class is the REST representation of the CCLicense URL String object and acts as a data object
 * for the SubmissionCCLicenseUrlRest class.
 */
public class SubmissionCCLicenseUrlRest extends BaseObjectRest<String> {
    public static final String NAME = "submissioncclicenseUrl";
    public static final String PLURAL = "submissioncclicenseUrls";
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;


    private String url;

    @JsonIgnore
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return SubmissionCCLicenseUrlRest.CATEGORY;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
