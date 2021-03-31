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
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Manage Groups Feature. It can be used to verify if the current user can manage groups in any community or
 * collection in the site.
 *
 * Authorization is granted if the current user has ADMIN permissions for any Community or Collection anywhere in the
 * site AND the corresponding core.authorization.* properties are set to true.
 */
@Component
@AuthorizationFeatureDocumentation(name = ManageGroupsFeature.NAME, description =
        "It can be used to verify if the current user can manage groups in any community or collection in the site")
public class ManageGroupsFeature implements AuthorizationFeature {

    public final static String NAME = "canManageGroups";

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof SiteRest) {
            if (authorizeService.isAdmin(context)) {
                return true;
            }

            if ((AuthorizeConfiguration.canCommunityAdminManagePolicies() ||
                AuthorizeConfiguration.canCommunityAdminManageAdminGroup()) &&
                authorizeService.isCommunityAdmin(context)) {
                return true;
            }

            if (
                (AuthorizeConfiguration.canCommunityAdminManageCollectionPolicies() ||
                    AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters() ||
                    AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows() ||
                    AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup())
                    && authorizeService.isCollectionAdmin(context)) {
                return true;
            }
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
