/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The move feature. It can be used to verify if an item can be moved to a different collection.
 *
 * Authorization is granted if the current user has WRITE permissions on the given item and REMOVE permissions on the
 * item’s owning collection
 */
@Component
@AuthorizationFeatureDocumentation(name = MoveFeature.NAME,
    description = "It can be used to verify if an item can be moved to a different collection")
public class MoveFeature implements AuthorizationFeature {

    Logger log = LogManager.getLogger();

    public final static String NAME = "canMove";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;
    @Autowired
    private Utils utils;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            if (!authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.WRITE)) {
                return false;
            }

            DSpaceObject owningObject = itemService.getParentObject(context,
                (Item)utils.getDSpaceAPIObjectFromRest(context, object));

            if (!(owningObject instanceof Collection)) {
                log.error("The partent object of item " + object.getType() + " is not a collection");
                return false;
            }

            return authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), owningObject,
                Constants.REMOVE, true);
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
