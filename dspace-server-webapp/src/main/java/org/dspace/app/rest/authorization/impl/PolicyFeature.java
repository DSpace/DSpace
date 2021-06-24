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
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The policy feature. It can be used by administrators (or community/collection delegate) to manage resource policies
 *
 * Authorization is granted
 * - for the site if the current user is administrator
 * - for other objects if the current user has ADMIN permissions on the object
 */
@Component
@AuthorizationFeatureDocumentation(name = PolicyFeature.NAME,
    description = "It can be used to verify if the resourcepolicies of the specified objects can be managed")
public class PolicyFeature implements AuthorizationFeature {

    public static final String NAME = "canManagePolicies";

    @Autowired
    AuthorizeService authService;
    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object != null) {
            try {
                if (object instanceof SiteRest) {
                    return authService.isAdmin(context);
                }
                if (object instanceof CommunityRest) {
                    AuthorizeUtil.authorizeManageCommunityPolicy(context,
                        (Community)utils.getDSpaceAPIObjectFromRest(context, object));
                    return true;
                }
                if (object instanceof CollectionRest) {
                    AuthorizeUtil.authorizeManageCollectionPolicy(context,
                        (Collection) utils.getDSpaceAPIObjectFromRest(context, object));
                    return true;
                }
                if (object instanceof ItemRest) {
                    AuthorizeUtil.authorizeManageItemPolicy(context,
                        (Item)utils.getDSpaceAPIObjectFromRest(context, object));
                    return true;
                }
                if (object instanceof BundleRest) {
                    AuthorizeUtil.authorizeManageBundlePolicy(context,
                        (Bundle)utils.getDSpaceAPIObjectFromRest(context, object));
                    return true;
                }
                if (object instanceof BitstreamRest) {
                    AuthorizeUtil.authorizeManageBitstreamPolicy(context,
                        (Bitstream)utils.getDSpaceAPIObjectFromRest(context, object));
                    return true;
                }
            } catch (AuthorizeException e) {
                return false;
            }
        }

        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            BundleRest.CATEGORY + "." + BundleRest.NAME,
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME
        };
    }
}
