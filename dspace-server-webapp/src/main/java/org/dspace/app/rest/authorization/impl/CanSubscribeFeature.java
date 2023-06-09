/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import static org.dspace.core.Constants.READ;

import java.sql.SQLException;
import java.util.Objects;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can subscribe to a DSpace object
 *
 * @author Alba Aliu (alba.aliu at atis.al)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSubscribeFeature.NAME,
        description = "Used to verify if the given user can subscribe to a DSpace object")
public class CanSubscribeFeature implements AuthorizationFeature {

    public static final String NAME = "canSubscribeDso";

    @Autowired
    private Utils utils;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (Objects.isNull(context.getCurrentUser())) {
            return false;
        }
        DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
        return authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), dSpaceObject, READ, true);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME
        };
    }

}