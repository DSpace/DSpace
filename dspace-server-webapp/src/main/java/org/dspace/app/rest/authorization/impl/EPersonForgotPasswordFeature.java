/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Checks if the user provided is allowed to request a password reset.
 * If none user specified, checks if the current context is allowed to set the password.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component
@AuthorizationFeatureDocumentation(name = EPersonForgotPasswordFeature.NAME,
    description = "It can be used to check password reset for an eperson")
public class EPersonForgotPasswordFeature implements AuthorizationFeature {

    public static final String NAME = "epersonForgotPassword";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        boolean isEperson = object instanceof EPersonRest;
        boolean isSite = object instanceof SiteRest;
        if (!isEperson && !isSite) {
            return false;
        }
        if (!AuthorizeUtil.authorizeForgotPassword()) {
            return false;
        }
        if (isEperson) {
            return AuthorizeUtil.authorizeUpdatePassword(context, ((EPersonRest) object).getEmail());
        }
        return true;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            EPersonRest.CATEGORY + "." + EPersonRest.NAME
        };
    }

}
