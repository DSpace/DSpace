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
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The canCreateCommunities feature.
 * It can be used to verify if a user has access to create a new community within a specific parent community or site.
 */
@Component
@AuthorizationFeatureDocumentation(name = CreateCommunityFeature.NAME,
        description = "It can be used to verify if a user has access to create a new community within a specific parent"
                + " community or site")
public class CreateCommunityFeature implements AuthorizationFeature {

    public static final String NAME = "canCreateCommunities";

    @Autowired
    AuthorizeService authService;

    @Autowired
    Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object != null) {
            if (object instanceof CommunityRest) {
                Community community = (Community) utils.getDSpaceAPIObjectFromRest(context, object);
                return authService.authorizeActionBoolean(context, community, Constants.ADD);
            }
            if (object instanceof SiteRest) {
                Site site = (Site) utils.getDSpaceAPIObjectFromRest(context, object);
                return authService.authorizeActionBoolean(context, site, Constants.ADD);
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            CommunityRest.CATEGORY + "." + CommunityRest.NAME
        };
    }
}
