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
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The QA Event feature. It can be used to verify if Quality Assurance can be seen.
 *
 * Authorization is granted if the current user has READ permissions on the given bitstream.
 */
@Component
@AuthorizationFeatureDocumentation(name = QAAuthorizationFeature.NAME,
        description = "It can be used to verify if the user can manage Quality Assurance events")
public class QAAuthorizationFeature implements AuthorizationFeature {
    public final static String NAME = "canSeeQA";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        return configurationService.getBooleanProperty("qaevents.enabled", false);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
