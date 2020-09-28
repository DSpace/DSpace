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
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The ManageTemplateItem feature. It can be used to verify if a collection template item can be created, deleted,
 * viewed or edited.
 *
 * Authorization is granted if the current user has ADMIN permissions on the given collection and the configuration
 * allows the collection admin to be the template item admin, OR the current user is the admin of the collection's
 * template item.
 */
@Component
@AuthorizationFeatureDocumentation(name = ManageTemplateItemFeature.NAME,
    description = "It can be used to verify if a collection template item can be created, deleted, viewed or edited")
public class ManageTemplateItemFeature implements AuthorizationFeature {

    public final static String NAME = "canManageTemplateItem";

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof CollectionRest) {
            try {
                AuthorizeUtil.authorizeManageTemplateItem(context,
                    (Collection)utils.getDSpaceAPIObjectFromRest(context, object));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            CollectionRest.CATEGORY + "." + CollectionRest.NAME
        };
    }
}
