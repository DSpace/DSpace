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
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = EPersonRegistrationFeature.NAME,
    description = "It can be used to register an eperson")
public class EPersonRegistrationFeature implements AuthorizationFeature {

    public static final String NAME = "epersonRegistration";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RequestService requestService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!(object instanceof SiteRest)) {
            return false;
        }
        if (configurationService.getBooleanProperty("user.registration", true)) {
            return authenticationService
                .allowSetPassword(context, requestService.getCurrentRequest().getHttpServletRequest(), null);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {SiteRest.CATEGORY + "." + SiteRest.NAME};
    }
}
