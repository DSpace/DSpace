/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static java.util.List.of;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.profile.service.ProfileSynchronizationWithOrcidConfigurator;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ProfileSynchronizationWithOrcidConfigurator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ProfileSynchronizationWithOrcidConfiguratorImpl implements ProfileSynchronizationWithOrcidConfigurator {

    @Autowired
    private ItemService itemService;

    @Override
    public void configureProfile(Context context, ResearcherProfile profile, OrcidTokenResponseDTO token)
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
        OrcidEntitySynchronizationPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-publications", of(value.name()));
    }

    @Override
    public void setProjectPreference(Context context, ResearcherProfile researcherProfile,
        OrcidEntitySynchronizationPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-projects", of(value.name()));

    }

    @Override
    public void setProfilePreference(Context context, ResearcherProfile researcherProfile,
        List<OrcidProfileSynchronizationPreference> values) throws SQLException {

        List<String> valuesAsString = values.stream()
            .map(OrcidProfileSynchronizationPreference::name)
            .collect(Collectors.toList());

        updatePreferenceForSynchronizingWithOrcid(context, researcherProfile, "sync-profile", valuesAsString);

    }

    @Override
    public void setSynchronizationMode(Context context, ResearcherProfile researcherProfile,
        OrcidSynchronizationMode value) throws SQLException {

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

}
