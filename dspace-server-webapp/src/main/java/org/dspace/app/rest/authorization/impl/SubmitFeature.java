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
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = SubmitFeature.NAME,
    description = "It can be used to verify if a user has rights to submit anything.")
public class SubmitFeature implements AuthorizationFeature {
    public static final String NAME = "canSubmit";

    @Autowired
    AuthorizeService authService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        if (object instanceof SiteRest) {
            // Check whether the user has permission to add to any collection
            return collectionService.countCollectionsWithSubmit("", context, null) > 0;
        } else if (object instanceof CollectionRest) {
            // Check whether the user has permission to add to the given collection
            Collection collection = (Collection) utils.getDSpaceAPIObjectFromRest(context, object);
            return authService.authorizeActionBoolean(context, collection, Constants.ADD);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
