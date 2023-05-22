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
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = EditItemFeature.NAME,
    description = "It can be used to verify if a user has rights to edit any item.")
public class EditItemFeature implements AuthorizationFeature {
    public static final String NAME = "canEditItem";
    @Autowired
    AuthorizeService authService;
    @Autowired
    ItemService itemService;

    @Autowired
    Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        if (object instanceof SiteRest) {
            return itemService.countItemsWithEdit(context) > 0;
        } else if (object instanceof ItemRest) {
            Item item = (Item) utils.getDSpaceAPIObjectFromRest(context, object);
            return authService.authorizeActionBoolean(context, item, Constants.WRITE);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
