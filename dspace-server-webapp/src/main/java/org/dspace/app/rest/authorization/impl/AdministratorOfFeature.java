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
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The administrator feature. It can be used for verify that an user has access
 * to the administrative features of the repository or of a specific community and collection.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AdministratorOfFeature.NAME,
        description = "It can be used for verify that an user has access "
                    + "to the administrative features of the repository or of a specific community and collection")
public class AdministratorOfFeature implements AuthorizationFeature {

    public static final String NAME = "administratorOf";

    @Autowired
    AuthorizeService authService;
    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object != null) {
            if (object instanceof CommunityRest) {
                Community community = (Community) utils.getDSpaceAPIObjectFromRest(context, object);
                return authService.isAdmin(context, community);
            }
            if (object instanceof CollectionRest) {
                Collection collection = (Collection) utils.getDSpaceAPIObjectFromRest(context, object);
                return authService.isAdmin(context, collection);
            }
            if (object instanceof ItemRest) {
                Item item = (Item) utils.getDSpaceAPIObjectFromRest(context, object);
                return authService.isAdmin(context, item);
            }
        }
        return authService.isAdmin(context);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            ItemRest.CATEGORY + "." + ItemRest.NAME
            };
    }
}