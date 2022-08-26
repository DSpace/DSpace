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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The create bundle feature. It can be used to verify if bundles can be created in a specific item.
 *
 * Authorization is granted if the current user has ADD & WRITE permissions on the given item
 */
@Component
@AuthorizationFeatureDocumentation(name = CreateBundleFeature.NAME,
    description = "It can be used to verify if bundles can be created in a specific item")
public class CreateBundleFeature implements AuthorizationFeature {

    public final static String NAME = "canCreateBundle";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            if (!authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.WRITE)) {
                return false;
            }
            return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.ADD);
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
