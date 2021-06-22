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
 * The authentication token REST HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 */
public class AuthenticationTokenRest extends RestAddressableModel {
    public static final String NAME = "shortlivedtoken";
    public static final String CATEGORY = RestAddressableModel.AUTHENTICATION;

    private String token;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
