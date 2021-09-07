/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The manage mapped items feature. It can be used to verify if mapped items can be listed, searched, added and removed.
 *
 * Authorization is granted if the current user has ADD and WRITE permissions on the given Collection.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanManageMappingsFeature.NAME,
    description = "It can be used to verify if mapped items can be listed, searched, added and removed")
public class CanManageMappingsFeature implements AuthorizationFeature {

    public final static String NAME = "canManageMappings";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof CollectionRest) {
            Collection collection = (Collection)utils.getDSpaceAPIObjectFromRest(context, object);

            if (authorizeService.authorizeActionBoolean(context, collection, Constants.WRITE)
                && authorizeService.authorizeActionBoolean(context, collection, Constants.ADD)) {
                return true;
            }
        }
        if (object instanceof ItemRest) {
            Item item = itemService.find(context, UUID.fromString(((ItemRest) object).getUuid()));
            if (!authorizeService.authorizeActionBoolean(context, item, Constants.WRITE)) {
                return false;
            }
            try {
                Optional<Collection> collections = collectionService.findCollectionsWithSubmit(StringUtils.EMPTY,
                                                 context, null, 0, Integer.MAX_VALUE)
                                                .stream()
                                                .filter(c -> !c.getID().equals(item.getOwningCollection().getID()))
                                                .filter(c -> {
                                                    try {
                                                        return collectionService.canEditBoolean(context, c);
                                                    } catch (SQLException e) {
                                                        throw new RuntimeException(e.getMessage(), e);
                                                    }
                                                })
                                                .findFirst();
                return collections.isPresent();
            } catch (SearchServiceException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
