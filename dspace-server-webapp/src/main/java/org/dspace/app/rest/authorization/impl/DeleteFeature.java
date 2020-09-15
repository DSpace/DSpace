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
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The delete feature. It can be used to verify if specific content can be deleted/expunged.
 *
 * Authorization is granted
 * - for a bitstream if the current used has REMOVE permissions on both the Item and the Bundle
 * - for a bundle if the current user has REMOVE permissions on the Item
 * - for an item if the current user has REMOVE permissions on the collection AND and DELETE permissions on the item
 * - for a collection if the current user has REMOVE permissions on the community
 * - for a community with a parent community if the current user has REMOVE permissions on the parent community
 * - for a community without a parent community if the current user has DELETE permissions on the current community
 * - for other objects if the current user has REMOVE permissions on the parent object if there is one. Otherwise if the
 *      current user has DELETE permissions on the current object
 */
@Component
@AuthorizationFeatureDocumentation(name = DeleteFeature.NAME,
    description = "It can be used to verify if specific content can be deleted/expunged")
public class DeleteFeature implements AuthorizationFeature {

    public final static String NAME = "canDelete";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;
    @Autowired
    private Utils utils;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ContentServiceFactory contentServiceFactory;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof BaseObjectRest) {
            if (object.getType().equals(WorkspaceItemRest.NAME)) {
                object = ((WorkspaceItemRest)object).getItem();
            }

            DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            DSpaceObject parentObject = getParentObject(context, dSpaceObject);

            switch (object.getType()) {
                case BitstreamRest.NAME:
                    return (
                        authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), parentObject,
                            Constants.REMOVE, true)
                        && authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), dSpaceObject,
                            Constants.REMOVE, true)
                        );
                case ItemRest.NAME:
                    return (
                        authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), parentObject,
                            Constants.REMOVE, true)
                        && authorizeServiceRestUtil.authorizeActionBoolean(context, object,
                            DSpaceRestPermission.DELETE)
                        );
                case CollectionRest.NAME:
                case CommunityRest.NAME:
                case BundleRest.NAME:
                case WorkspaceItemRest.NAME:
                case EPersonRest.NAME:
                case GroupRest.NAME:
                default:
                    if (parentObject != null) {
                        return authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), parentObject,
                            Constants.REMOVE, true);
                    }

                    return authorizeServiceRestUtil.authorizeActionBoolean(context, object,
                        DSpaceRestPermission.DELETE);
            }
        }
        return false;
    }

    private DSpaceObject getParentObject(Context context, DSpaceObject object) throws SQLException {
        DSpaceObject parentObject
            = contentServiceFactory.getDSpaceObjectService(object.getType()).getParentObject(context, object);
        if (object.getType() == Constants.ITEM && parentObject == null) {
            Item item = (Item) object;
            parentObject = item.getOwningCollection();
            WorkspaceItem byItem = ContentServiceFactory.getInstance()
                                        .getWorkspaceItemService()
                                        .findByItem(context, item);
            if (byItem != null) {
                parentObject = byItem.getCollection();
            }
        }
        return parentObject;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            BundleRest.CATEGORY + "." + BundleRest.NAME,
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
            WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME,
            EPersonRest.CATEGORY + "." + EPersonRest.NAME,
            GroupRest.CATEGORY + "." + GroupRest.NAME
        };
    }
}
