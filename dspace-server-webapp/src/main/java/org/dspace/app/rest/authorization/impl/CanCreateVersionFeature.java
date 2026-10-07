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
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The create version feature. It can be used to verify if the user can create the version of an Item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanCreateVersionFeature.NAME,
    description = "It can be used to verify if the user can create a new version of an Item")
public class CanCreateVersionFeature implements AuthorizationFeature {

    public static final String NAME = "canCreateVersion";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            if (!configurationService.getBooleanProperty("versioning.enabled", true)) {
                return false;
            }
            EPerson currentUser = context.getCurrentUser();
            if (Objects.isNull(currentUser)) {
                return false;
            }
            Item item = itemService.find(context, UUID.fromString(((ItemRest) object).getUuid()));
            if (Objects.nonNull(item)) {
                if (authorizeService.isAdmin(context, item)) {
                    return true;
                }
                if (configurationService.getBooleanProperty("versioning.submitterCanCreateNewVersion")) {
                    EPerson submitter = item.getSubmitter();
                    return Objects.nonNull(submitter) && currentUser.getID().equals(submitter.getID());
                }
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
