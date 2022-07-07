/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.profile.service.ResearcherProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can claim the given item. An item can be claimed
 * only if the show claim is enabled for it (see
 * {@link org.dspace.app.rest.authorization.impl.ShowClaimItemFeature}).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
@AuthorizationFeatureDocumentation(name = CanClaimItemFeature.NAME,
    description = "Used to verify if the current user is able to claim this item as their profile. "
        + "Only available if the current item is not already claimed.")
public class CanClaimItemFeature implements AuthorizationFeature {

    public static final String NAME = "canClaimItem";

    private static final Logger LOG = LoggerFactory.getLogger(CanClaimItemFeature.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!(object instanceof ItemRest) || context.getCurrentUser() == null) {
            return false;
        }

        String id = ((ItemRest) object).getId();
        Item item = itemService.find(context, UUID.fromString(id));

        return researcherProfileService.hasProfileType(item)
            && hasNotOwner(item)
            && hasNotAlreadyAProfile(context)
            && haveSameEmail(item, context.getCurrentUser());
    }

    private boolean hasNotAlreadyAProfile(Context context) {
        try {
            return researcherProfileService.findById(context, context.getCurrentUser().getID()) == null;
        } catch (SQLException | AuthorizeException e) {
            LOG.warn("Error while checking if eperson has a ResearcherProfileAssociated: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean hasNotOwner(Item item) {
        return StringUtils.isBlank(itemService.getMetadata(item, "dspace.object.owner"));
    }

    private boolean haveSameEmail(Item item, EPerson currentUser) {
        return itemService.getMetadataByMetadataString(item, "person.email").stream()
            .map(MetadataValue::getValue)
            .filter(StringUtils::isNotBlank)
            .anyMatch(email -> email.equalsIgnoreCase(currentUser.getEmail()));
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }

}
