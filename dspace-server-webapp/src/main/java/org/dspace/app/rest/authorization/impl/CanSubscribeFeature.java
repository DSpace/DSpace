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
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can subscribe to a dataspace object
 *
 * @author Alba Aliu (alba.aliu at atis.al)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSubscribeFeature.NAME,
        description = "Used to verify if the given user can subscribe to a dataspace object")
public class CanSubscribeFeature implements AuthorizationFeature {

    public static final String NAME = "canSubscribeDso";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
        return authorizeService.authorizeActionBoolean(context, context.getCurrentUser(),
                dSpaceObject, Constants.READ, true);

    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public String[] getSupportedTypes() {
        return new String[]{CollectionRest.CATEGORY + "." + CollectionRest.NAME, ItemRest.CATEGORY + "." +
                ItemRest.NAME, CommunityRest.CATEGORY + "." + CommunityRest.NAME,
                SiteRest.CATEGORY + "." + SiteRest.NAME,  EPersonRest.CATEGORY + "." + EPersonRest.NAME,
                BundleRest.CATEGORY + "." + BundleRest.NAME, BitstreamRest.CATEGORY + "." + BitstreamRest.NAME
        };
    }
}