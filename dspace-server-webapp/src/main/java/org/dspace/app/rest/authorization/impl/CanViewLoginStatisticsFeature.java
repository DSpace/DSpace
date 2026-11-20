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
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * Implementation of {@link AuthorizationFeature} defining whether or not login statistics
 * can be viewed.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
@Component
@AuthorizationFeatureDocumentation(name = CanViewLoginStatisticsFeature.NAME,
    description = "Used to define if login statistics can be viewed")
public class CanViewLoginStatisticsFeature implements AuthorizationFeature {

    public static final String NAME = "canViewLoginStatistics";

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        //TODO: currently only administrators can see login statistics
        return authorizeService.isAdmin(context);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
