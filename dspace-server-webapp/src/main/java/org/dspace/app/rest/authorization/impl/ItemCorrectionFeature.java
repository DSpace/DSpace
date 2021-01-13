/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.content.Item;
import org.dspace.content.edit.CorrectItemMode;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AuthorizationFeature} to evaluate if the current
 * user is allowed to request a correction for the given item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
@AuthorizationFeatureDocumentation(name = ItemCorrectionFeature.NAME,
    description = "It can be used for verify that an user is enabled to correct the given item")
public class ItemCorrectionFeature implements AuthorizationFeature {

    public static final String NAME = "canCorrectItem";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisSecurityService crisSecurityService;

    @Autowired
    @Qualifier("correctItemModesMap")
    private Map<String, List<CorrectItemMode>> correctItemModesMap;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!(object instanceof ItemRest)) {
            return false;
        }

        if (!configurationService.getBooleanProperty("item-correction.enabled", true)) {
            return false;
        }

        if (configurationService.getBooleanProperty("item-correction.permit-all", false)) {
            return true;
        }

        return isAuthorizedToCorrectItem(context, (ItemRest) object);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }

    private boolean isAuthorizedToCorrectItem(Context context, ItemRest itemRest) throws SQLException {

        Item item = itemService.find(context, UUID.fromString(itemRest.getUuid()));
        if (item == null) {
            throw new IllegalArgumentException("No item found with the given id: " + itemRest.getUuid());
        }

        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        if (!correctItemModesMap.containsKey(entityType)) {
            return false;
        }

        List<CorrectItemMode> correctItemModes = correctItemModesMap.get(entityType);
        for (CorrectItemMode correctItemMode : correctItemModes) {
            if (crisSecurityService.hasAccess(context, item, context.getCurrentUser(), correctItemMode)) {
                return true;
            }
        }

        return false;
    }

}
