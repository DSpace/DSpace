/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login.impl;

import static org.apache.commons.collections4.IteratorUtils.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.service.ResearcherProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link PostLoggedInAction} that perform an automatic claim
 * between the logged eperson and possible profiles without eperson present in
 * the system. This pairing between eperson and profile is done starting from
 * the configured metadata of the logged in user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileAutomaticClaim implements PostLoggedInAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(ResearcherProfileAutomaticClaim.class);

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * The field of the eperson to search for.
     */
    private final String ePersonField;

    /**
     * The field of the profile item to search.
     */
    private final String profileField;

    public ResearcherProfileAutomaticClaim(String ePersonField, String profileField) {
        Assert.notNull(ePersonField, "An eperson field is required to perform automatic claim");
        Assert.notNull(profileField, "An profile field is required to perform automatic claim");
        this.ePersonField = ePersonField;
        this.profileField = profileField;
    }

    @Override
    public void loggedIn(Context context) {

        if (isBlank(researcherProfileService.getProfileType())) {
            return;
        }

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            claimProfile(context, currentUser);
        } catch (SQLException | AuthorizeException e) {
            LOGGER.error("An error occurs during the profile claim by email", e);
        }

    }

    private void claimProfile(Context context, EPerson currentUser) throws SQLException, AuthorizeException {

        UUID id = currentUser.getID();
        String fullName = currentUser.getFullName();

        if (currentUserHasAlreadyResearcherProfile(context)) {
            return;
        }

        Item item = findClaimableProfile(context, currentUser);
        if (item != null) {
            itemService.addMetadata(context, item, "dspace", "object", "owner",
                                    null, fullName, id.toString(), CF_ACCEPTED);
        }

    }

    private boolean currentUserHasAlreadyResearcherProfile(Context context) throws SQLException, AuthorizeException {
        return researcherProfileService.findById(context, context.getCurrentUser().getID()) != null;
    }

    private Item findClaimableProfile(Context context, EPerson currentUser) throws SQLException, AuthorizeException {

        String value = getValueToSearchFor(context, currentUser);
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        List<Item> items = toList(itemService.findArchivedByMetadataField(context, profileField, value)).stream()
            .filter(this::hasNotOwner)
            .filter(researcherProfileService::hasProfileType)
            .collect(Collectors.toList());

        return items.size() == 1 ? items.get(0) : null;
    }

    private String getValueToSearchFor(Context context, EPerson currentUser) {
        if ("email".equals(ePersonField)) {
            return currentUser.getEmail();
        }
        return ePersonService.getMetadataFirstValue(currentUser, new MetadataFieldName(ePersonField), Item.ANY);
    }

    private boolean hasNotOwner(Item item) {
        return CollectionUtils.isEmpty(itemService.getMetadata(item, "dspace", "object", "owner", Item.ANY));
    }

}
