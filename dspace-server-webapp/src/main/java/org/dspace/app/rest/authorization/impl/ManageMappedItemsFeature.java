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
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The manage mapped items feature. It can be used to verify if mapped items can be listed, searched, added and removed.
 *
 * Authorization is granted if the current user has ADD and WRITE permissions on the given Collection.
 */
@Component
@AuthorizationFeatureDocumentation(name = ManageMappedItemsFeature.NAME,
    description = "It can be used to verify if mapped items can be listed, searched, added and removed")
public class ManageMappedItemsFeature implements AuthorizationFeature {

    public final static String NAME = "canManageMappedItems";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof CollectionRest) {
            Collection collection = (Collection)utils.getDSpaceAPIObjectFromRest(context, object);

            if (authorizeService.authorizeActionBoolean(context, collection, Constants.WRITE)
                && authorizeService.authorizeActionBoolean(context, collection, Constants.ADD)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            CollectionRest.CATEGORY + "." + CollectionRest.NAME
        };
    }
}
