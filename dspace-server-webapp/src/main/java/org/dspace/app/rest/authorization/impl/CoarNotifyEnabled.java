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
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = CoarNotifyEnabled.NAME,
        description = "It can be used to verify if the user can see the coar notify protocol is enabled")
public class CoarNotifyEnabled implements AuthorizationFeature {

    public final static String NAME = "coarNotifyEnabled";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        return configurationService.getBooleanProperty("ldn.enabled", true);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{ SiteRest.CATEGORY + "." + SiteRest.NAME };
    }

}
