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
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The view versions feature. It can be used to verify if the user can view the versions of an Item.
 *
 * In case DSpace is configured to only show versions to administrators, authorization is granted if the
 * current user is the object's admin. Otherwise, authorization is granted if the current user can view the object.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSeeVersionsFeature.NAME,
    description = "It can be used to verify if the user can view the versions of an Item")
public class CanSeeVersionsFeature implements AuthorizationFeature {

    public final static String NAME = "canSeeVersions";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            if (configurationService.getBooleanProperty("versioning.item.history.view.admin",
                false)) {
                return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.ADMIN);
            } else {
                return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.READ);
            }

        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
