/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static java.util.List.of;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.dspace.app.profile.OrcidEntitySyncPreference.DISABLED;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.profile.service.ProfileOrcidSynchronizationService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ProfileOrcidSynchronizationService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ProfileOrcidSynchronizationServiceImpl implements ProfileOrcidSynchronizationService {

    @Autowired
    private ItemService itemService;

    @Override
    public void linkProfile(Context context, ResearcherProfile profile, OrcidTokenResponseDTO token)
        throws SQLException {

        String orcid = token.getOrcid();
        String accessToken = token.getAccessToken();
        String refreshToken = token.getRefreshToken();
        String[] scopes = token.getScopeAsArray();

        Item item = profile.getItem();

        itemService.setMetadataSingleValue(context, item, "person", "identifier", "orcid", null, orcid);
        itemService.setMetadataSingleValue(context, item, "cris", "orcid", "access-token", null, accessToken);
        itemService.setMetadataSingleValue(context, item, "cris", "orcid", "refresh-token", null, refreshToken);
        itemService.clearMetadata(context, item, "cris", "orcid", "scope", null);
        for (String scope : scopes) {
            itemService.addMetadata(context, item, "cris", "orcid", "scope", null, scope);
        }

    }

    @Override
    public void setPublicationPreference(Context context, ResearcherProfile researcherProfile,
        OrcidEntitySyncPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-publications", of(value.name()));
    }

    @Override
    public void setProjectPreference(Context context, ResearcherProfile researcherProfile,
        OrcidEntitySyncPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-projects", of(value.name()));

    }

    @Override
    public void setProfilePreference(Context context, ResearcherProfile researcherProfile,
        List<OrcidProfileSyncPreference> values) throws SQLException {

        List<String> valuesAsString = values.stream()
            .map(OrcidProfileSyncPreference::name)
            .collect(Collectors.toList());

        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-profile", valuesAsString);

    }

    @Override
    public void setSynchronizationMode(Context context, ResearcherProfile researcherProfile,
        OrcidSyncMode value) throws SQLException {

        if (!researcherProfile.isLinkedToOrcid()) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                + "synchronization because it is not linked to any ORCID account: " + researcherProfile.getId());
        }

        Item item = researcherProfile.getItem();
        itemService.setMetadataSingleValue(context, item, "cris", "orcid", "sync-mode", null, value.name());
    }

    private void updatePreferenceForSynchronizingWithOrcid(Context context, ResearcherProfile researcherProfile,
        String metadataQualifier, List<String> values) throws SQLException {

        if (!researcherProfile.isLinkedToOrcid()) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                + "synchronization because it is not linked to any ORCID account: " + researcherProfile.getId());
        }

        Item item = researcherProfile.getItem();

        itemService.clearMetadata(context, item, "cris", "orcid", metadataQualifier, Item.ANY);
        for (String value : values) {
            itemService.addMetadata(context, item, "cris", "orcid", metadataQualifier, null, value);
        }

    }

    @Override
    public boolean isSynchronizationEnabled(ResearcherProfile profile, Item item) {

        String entityType = itemService.getEntityType(item);
        if (entityType == null) {
            return false;
        }

        switch (entityType) {
            case "Person":
                return profile.getItem().equals(item) && !isEmpty(profile.getOrcidSynchronizationProfilePreferences());
            case "Publication":
                return !profile.getOrcidSynchronizationPublicationsPreference().equals(DISABLED.name());
            case "Project":
                return !profile.getOrcidSynchronizationProjectsPreference().equals(DISABLED.name());
            default:
                return false;
        }

    }

    @Override
    public boolean isSynchronizationEnabled(Item profileItem, Item item) {
        try {
            return isSynchronizationEnabled(new ResearcherProfile(profileItem), item);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

}
