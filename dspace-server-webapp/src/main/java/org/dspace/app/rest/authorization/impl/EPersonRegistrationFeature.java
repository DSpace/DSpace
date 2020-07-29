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
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The EPerson Registration feature. It's able to be used on site objects if the user.registration property is set to
 * true. If it's set to true, it'll check if the current context is allowed to set the password.
 */
@Component
@AuthorizationFeatureDocumentation(name = EPersonRegistrationFeature.NAME,
    description = "It can be used to register an eperson")
public class EPersonRegistrationFeature implements AuthorizationFeature {

    public static final String NAME = "epersonRegistration";

    @Autowired
    private RequestService requestService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!(object instanceof SiteRest)) {
            return false;
        }
        if (!AuthorizeUtil.authorizeNewAccountRegistration(context,
                                                      requestService.getCurrentRequest().getHttpServletRequest())) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {SiteRest.CATEGORY + "." + SiteRest.NAME};
    }
}
