/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can claim the given item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
@AuthorizationFeatureDocumentation(name = CanClaimItemFeature.NAME,
    description = "Used to verify if the given user can request the claim of an item")
public class CanClaimItemFeature implements AuthorizationFeature {

    public static final String NAME = "canClaimItem";

    @Autowired
    private ItemService itemService;

    @Autowired
    private ShowClaimItemFeature showClaimItemFeature;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!showClaimItemFeature.isAuthorized(context, object)) {
            return false;
        }

        if (!(object instanceof ItemRest) || Objects.isNull(context.getCurrentUser())) {
            return false;
        }

        String id = ((ItemRest) object).getId();
        Item item = itemService.find(context, UUID.fromString(id));

        return hasNotOwner(item);
    }

    private boolean hasNotOwner(Item item) {
        return StringUtils.isBlank(itemService.getMetadata(item, "dspace.object.owner"));
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }

}
