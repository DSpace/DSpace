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

import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can request the claim of an item. Whether or not the
 * user can then make the claim is determined by the feature
 * {@link CanClaimItemFeature}.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = ShowClaimItemFeature.NAME,
    description = "Used to verify if the given user can request the claim of an item")
public class ShowClaimItemFeature implements AuthorizationFeature {

    public static final String NAME = "showClaimItem";
    private static final Logger LOG = LoggerFactory.getLogger(ShowClaimItemFeature.class);

    private final ItemService itemService;
    private final ResearcherProfileService researcherProfileService;

    @Autowired
    public ShowClaimItemFeature(ItemService itemService, ResearcherProfileService researcherProfileService) {
        this.itemService = itemService;
        this.researcherProfileService = researcherProfileService;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!(object instanceof ItemRest) || Objects.isNull(context.getCurrentUser())) {
            return false;
        }

        String id = ((ItemRest) object).getId();
        Item item = itemService.find(context, UUID.fromString(id));

        return researcherProfileService.hasProfileType(item) && hasNotAlreadyAProfile(context);
    }

    private boolean hasNotAlreadyAProfile(Context context) {
        try {
            return researcherProfileService.findById(context, context.getCurrentUser().getID()) == null;
        } catch (SQLException | AuthorizeException e) {
            LOG.warn("Error while checking if eperson has a ResearcherProfileAssociated: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {ItemRest.CATEGORY + "." + ItemRest.NAME};
    }
}
