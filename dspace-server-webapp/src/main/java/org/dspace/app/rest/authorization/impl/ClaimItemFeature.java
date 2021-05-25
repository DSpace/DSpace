/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if a given Item can be claimed or not by a given user.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = ClaimItemFeature.NAME,
    description = "Used to verify if for a given Profile (Person) Item, an user has claim rights")
public class ClaimItemFeature implements AuthorizationFeature {

    public static final String NAME = "canClaimItem";
    private static final Logger LOG = LoggerFactory.getLogger(ClaimItemFeature.class);

    private final ItemService itemService;
    private final ResearcherProfileService researcherProfileService;
    private final ConfigurationService configurationService;
    private final RelationshipService relationshipService;

    @Autowired
    public ClaimItemFeature(ItemService itemService,
                            ResearcherProfileService researcherProfileService,
                            ConfigurationService configurationService,
                            final RelationshipService relationshipService) {
        this.itemService = itemService;
        this.researcherProfileService = researcherProfileService;
        this.configurationService = configurationService;
        this.relationshipService = relationshipService;
    }

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!(object instanceof ItemRest) ||
                Objects.isNull(context.getCurrentUser()) ||
                hasAlreadyAProfileWithClone(context) ||
                Objects.isNull(configurationService.getArrayProperty("claimable.collection.uuid"))) {
            return false;
        }
        String id = ((ItemRest) object).getId();
        Item item = itemService.find(context, UUID.fromString(id));
        return claimable(context, item);
    }

    private boolean hasAlreadyAProfileWithClone(Context context) {
        try {

            ResearcherProfile profile = researcherProfileService.findById(context, context.getCurrentUser().getID());
            return Optional.ofNullable(profile)
                .map(p -> Objects.nonNull(p.getItem()))
                .orElse(false);

        } catch (SQLException | AuthorizeException e) {
            LOG.warn("Error while checking if eperson has a ResearcherProfileAssociated: {}",
                     e.getMessage(), e);
            return false;
        }
    }

    private boolean claimable(final Context context, Item item) throws SQLException {
        if (unClaimableEntityType(item) || alreadyInARelation(context, item)) {
            return false;
        }
        return supportedCollection(item);
    }

    private boolean alreadyInARelation(final Context context, final Item item) {

        return hasOwner(item);
    }

    private boolean hasOwner(final Item item) {
        return StringUtils.isNotBlank(itemService.getMetadata(item, "cris.owner"));
    }

    private boolean unClaimableEntityType(Item item) {
        List<String> claimableEntityTypes = Arrays.asList(
            configurationService.getArrayProperty("claimable.entityType")
                                                         );
        return itemService.getMetadataByMetadataString(item, "dspace.entity.type")
                          .stream()
                          .noneMatch(mv -> claimableEntityTypes.contains(mv.getValue()));
    }

    private boolean supportedCollection(Item item) {
        List<String> claimableItemCollections =
            Arrays.asList(configurationService.getArrayProperty("claimable.collection.uuid"));

        return item.getCollections()
                   .stream()
                   .map(c -> c.getID().toString())
                   .anyMatch(claimableItemCollections::contains);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {ItemRest.CATEGORY + "." + ItemRest.NAME};
    }
}
