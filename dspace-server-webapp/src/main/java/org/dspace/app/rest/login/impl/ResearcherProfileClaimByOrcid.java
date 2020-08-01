/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login.impl;

import static org.apache.commons.collections4.IteratorUtils.toList;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link PostLoggedInAction} that perform an automatic claim
 * between the logged eperson and possible profiles without eperson present in
 * the system. This pairing between eperson and profile is done starting from
 * the orcid of the logged in user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class ResearcherProfileClaimByOrcid implements PostLoggedInAction {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileClaimByOrcid.class);

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private ItemService itemService;

    @Override
    public void loggedIn(Context context, HttpServletRequest request) {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String orcid = (String) request.getAttribute("orcid");
        if (orcid == null) {
            return;
        }

        try {
            claimProfile(context, currentUser, orcid);
        } catch (SQLException | AuthorizeException e) {
            log.error("An error occurs during the profile claim by orcId", e);
        }

    }

    private void claimProfile(Context context, EPerson currentUser, String orcid)
        throws SQLException, AuthorizeException {

        UUID id = currentUser.getID();

        ResearcherProfile profile = researcherProfileService.findById(context, id);
        if (profile != null) {
            return;
        }

        String fullName = currentUser.getFullName();

        List<Item> items = toList(itemService.findByMetadataField(context, "crisrp", "orcid", null, orcid));

        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        if (items.size() > 1) {
            log.debug("Found more than one profile item with the crisp.orcid equals to " + orcid);
            return;
        }

        Item item = items.get(0);
        List<MetadataValue> metadata = itemService.getMetadata(item, "cris", "owner", null, Item.ANY);
        if (CollectionUtils.isEmpty(metadata)) {
            itemService.addMetadata(context, item, "cris", "owner", null, null, fullName, id.toString(), CF_ACCEPTED);
        } else if (metadata.size() == 1) {
            MetadataValue crisOwner = metadata.get(0);
            if (StringUtils.isBlank(crisOwner.getAuthority())) {
                crisOwner.setAuthority(id.toString());
                crisOwner.setValue(fullName);
            }
        } else {
            log.debug("Found a profile item (" + item.getID() + ") with multiple cris.owner");
        }

    }

}
