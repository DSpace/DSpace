/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

@LinksRest(links = {
        @LinkRest(
                name = ClarinLicenseResourceUserAllowanceRest.RESOURCE_MAPPING,
                method = "getResourceMapping"
        ),
        @LinkRest(
                name = ClarinLicenseResourceUserAllowanceRest.USER_REGISTRATION,
                method = "getUserRegistration"
        ),
        @LinkRest(
                name = ClarinLicenseResourceUserAllowanceRest.USER_METADATA,
                method = "getUserMetadata"
        )
})
public class ClarinLicenseResourceUserAllowanceRest extends BaseObjectRest<Integer> {
    // clarinlicenseresourceuserallowances
    public static final String NAME = "clarinlruallowance";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String RESOURCE_MAPPING = "resourceMapping";
    public static final String USER_REGISTRATION = "userRegistration";
    public static final String USER_METADATA = "userMetadata";

    private String token;

    public ClarinLicenseResourceUserAllowanceRest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
}
