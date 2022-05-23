/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * The canChangePassword feature. It can be used to verify if the user can change his password.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanChangePasswordFeature.NAME,
        description = "It can be used to verify if the user can change his password")
public class CanChangePasswordFeature implements AuthorizationFeature {

    public static final String NAME = "canChangePassword";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (context.getCurrentUser() != null && StringUtils.equals(context.getAuthenticationMethod(), "password")) {
            return true;
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            EPersonRest.CATEGORY + "." + EPersonRest.NAME
        };
    }

}