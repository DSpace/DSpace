/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The LoginOnBehalfOf feature. It can be used by Administrators when the webui.user.assumelogin property is set to true
 * to allow the admin to login as another user
 */
@Component
@AuthorizationFeatureDocumentation(name = LoginOnBehalfOfFeature.NAME,
    description = "It can be used by administrators to login on behalf of a different user")
public class LoginOnBehalfOfFeature implements AuthorizationFeature {

    public static final String NAME = "loginOnBehalfOf";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!StringUtils.equals(object.getType(), SiteRest.NAME)) {
            return false;
        }
        if (!authorizeService.isAdmin(context)) {
            return false;
        }
        if (!configurationService.getBooleanProperty("webui.user.assumelogin")) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { SiteRest.CATEGORY + "." + SiteRest.NAME };
    }

}
