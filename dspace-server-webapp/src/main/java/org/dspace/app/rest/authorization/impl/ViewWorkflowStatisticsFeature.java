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
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The view workflow statistics feature. It can be used to verify if statistics can be viewed.
 *
 * In case DSpace is configured to only show workflow statistics to administrators, authorization is granted if the
 * current user is the object's admin. Otherwise, authorization is granted if the current user can view the object.
 */
@Component
@AuthorizationFeatureDocumentation(name = ViewWorkflowStatisticsFeature.NAME,
    description = "It can be used to verify if the workflow statistics can be viewed")
public class ViewWorkflowStatisticsFeature implements AuthorizationFeature {

    public final static String NAME = "canViewWorkflowStatistics";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof SiteRest
            || object instanceof CommunityRest
            || object instanceof CollectionRest) {

            if (configurationService.getBooleanProperty("usage-statistics.authorization.admin.workflow", true)) {
                return authorizeService.isAdmin(context,
                    (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object));
            } else {
                return authorizeService.authorizeActionBoolean(context,
                    (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object), org.dspace.core.Constants.READ);
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME
        };
    }
}
