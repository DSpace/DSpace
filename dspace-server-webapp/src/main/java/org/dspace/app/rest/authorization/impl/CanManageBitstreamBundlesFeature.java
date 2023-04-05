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
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The manageBitstreamBundles feature. It can be used to verify
 * if the user can manage (ADD | REMOVE) the bundles of bitstreams of an Item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanManageBitstreamBundlesFeature.NAME,
    description = "It can be used to verify if the user can manage (ADD | REMOVE) the bundles of bitstreams of an Item")
public class CanManageBitstreamBundlesFeature implements AuthorizationFeature {

    public static final String NAME = "canManageBitstreamBundles";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            boolean hasRemovePermission = authorizeService.authorizeActionBoolean(context, context.getCurrentUser(),
                                                           dSpaceObject, Constants.REMOVE, true);
            boolean hasAddPermission = authorizeService.authorizeActionBoolean(context, context.getCurrentUser(),
                                                        dSpaceObject, Constants.ADD, true);
            return (hasRemovePermission && hasAddPermission);
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