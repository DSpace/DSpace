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
 * The Collection Admin feature. It can be used to verify if the current user is admin of any collection in the site.
 *
 * Authorization is granted if the current user has ADMIN permissions for any Collection anywhere in the site, or if the
 * current user is a site admin.
 */
@Component
@AuthorizationFeatureDocumentation(name = CollectionAdminFeature.NAME,
    description = "It can be used to verify if the current user is admin of any collection in the site")
public class CollectionAdminFeature implements AuthorizationFeature {

    public final static String NAME = "isCollectionAdmin";

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof SiteRest) {
            if (authorizeService.isAdmin(context)) {
                return true;
            }

            return authorizeService.isCollectionAdmin(context);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
