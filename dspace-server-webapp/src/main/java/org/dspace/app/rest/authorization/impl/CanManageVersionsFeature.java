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

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The manage versions feature. It can be used to verify
 * if the user can create/delete or update the version of an Item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanManageVersionsFeature.NAME,
    description = "It can be used to verify if the user can create/delete or update the version of an Item")
public class CanManageVersionsFeature implements AuthorizationFeature {

    public static final String NAME = "canManageVersions";

    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ConfigurationService configurationService;


    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            boolean isEnabled = configurationService.getBooleanProperty("versioning.enabled", true);
            if (!isEnabled || Objects.isNull(context.getCurrentUser())) {
                return false;
            }
            Item item = itemService.find(context, UUID.fromString(((ItemRest) object).getUuid()));
            if (Objects.nonNull(item)) {
                return authorizeService.isAdmin(context, item);
            }
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
