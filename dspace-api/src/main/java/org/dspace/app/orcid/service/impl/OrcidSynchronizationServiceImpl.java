/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.util.List.of;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.dspace.app.profile.OrcidEntitySyncPreference.DISABLED;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.profile.OrcidEntitySyncPreference;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.app.profile.OrcidSyncMode;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidSynchronizationService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidSynchronizationServiceImpl implements OrcidSynchronizationService {

    @Autowired
    private ItemService itemService;

    @Override
    public void linkProfile(Context context, Item profile, OrcidTokenResponseDTO token)
        throws SQLException {

        String orcid = token.getOrcid();
        String accessToken = token.getAccessToken();
        String refreshToken = token.getRefreshToken();
        String[] scopes = token.getScopeAsArray();

        itemService.setMetadataSingleValue(context, profile, "person", "identifier", "orcid", null, orcid);
        itemService.setMetadataSingleValue(context, profile, "cris", "orcid", "access-token", null, accessToken);
        itemService.setMetadataSingleValue(context, profile, "cris", "orcid", "refresh-token", null, refreshToken);
        itemService.clearMetadata(context, profile, "cris", "orcid", "scope", null);
        for (String scope : scopes) {
            itemService.addMetadata(context, profile, "cris", "orcid", "scope", null, scope);
        }

    }

    @Override
    public void setPublicationPreference(Context context, Item profile,
        OrcidEntitySyncPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, profile, "sync-publications", of(value.name()));
    }

    @Override
    public void setProjectPreference(Context context, Item profile,
        OrcidEntitySyncPreference value) throws SQLException {
        updatePreferenceForSynchronizingWithOrcid(context, profile, "sync-projects", of(value.name()));

    }

    @Override
    public void setProfilePreference(Context context, Item profile, List<OrcidProfileSyncPreference> values)
        throws SQLException {

        List<String> valuesAsString = values.stream()
            .map(OrcidProfileSyncPreference::name)
            .collect(Collectors.toList());

        updatePreferenceForSynchronizingWithOrcid(context, profile, "sync-profile", valuesAsString);

    }

    @Override
    public void setSynchronizationMode(Context context, Item profile,  OrcidSyncMode value) throws SQLException {

        if (!isLinkedToOrcid(profile)) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                + "synchronization because it is not linked to any ORCID account: " + profile.getID());
        }

        itemService.setMetadataSingleValue(context, profile, "cris", "orcid", "sync-mode", null, value.name());
    }

    private void updatePreferenceForSynchronizingWithOrcid(Context context, Item profile,
        String metadataQualifier, List<String> values) throws SQLException {

        if (!isLinkedToOrcid(profile)) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                + "synchronization because it is not linked to any ORCID account: " + profile.getID());
        }

        itemService.clearMetadata(context, profile, "cris", "orcid", metadataQualifier, Item.ANY);
        for (String value : values) {
            itemService.addMetadata(context, profile, "cris", "orcid", metadataQualifier, null, value);
        }

    }

    @Override
    public boolean isSynchronizationEnabled(Item profile, Item item) {

        String entityType = itemService.getEntityType(item);
        if (entityType == null) {
            return false;
        }

        switch (entityType) {
            case "Person":
                return profile.equals(item) && !isEmpty(getProfilePreferences(profile));
            case "Publication":
                return getPublicationsPreference(profile).filter(preference -> preference != DISABLED).isPresent();
            case "Project":
                return getProjectsPreference(profile).filter(preference -> preference != DISABLED).isPresent();
            default:
                return false;
        }

    }

    @Override
    public Optional<OrcidSyncMode> getSynchronizationMode(Item item) {
        return getMetadataValue(item, "cris.orcid.sync-mode")
            .map(metadataValue -> metadataValue.getValue())
            .filter(value -> isValidEnum(OrcidSyncMode.class, value))
            .map(value -> OrcidSyncMode.valueOf(value));
    }

    @Override
    public Optional<OrcidEntitySyncPreference> getPublicationsPreference(Item item) {
        return getMetadataValue(item, "cris.orcid.sync-publications")
            .map(metadataValue -> metadataValue.getValue())
            .filter(value -> isValidEnum(OrcidEntitySyncPreference.class, value))
            .map(value -> OrcidEntitySyncPreference.valueOf(value));
    }

    @Override
    public Optional<OrcidEntitySyncPreference> getProjectsPreference(Item item) {
        return getMetadataValue(item, "cris.orcid.sync-projects")
            .map(metadataValue -> metadataValue.getValue())
            .filter(value -> isValidEnum(OrcidEntitySyncPreference.class, value))
            .map(value -> OrcidEntitySyncPreference.valueOf(value));
    }

    @Override
    public List<OrcidProfileSyncPreference> getProfilePreferences(Item item) {
        return getMetadataValues(item, "cris.orcid.sync-profile")
            .map(MetadataValue::getValue)
            .filter(value -> isValidEnum(OrcidProfileSyncPreference.class, value))
            .map(value -> OrcidProfileSyncPreference.valueOf(value))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isLinkedToOrcid(Item item) {
        return getOrcidAccessToken(item).isPresent() && getOrcid(item).isPresent();
    }

    private Optional<String> getOrcidAccessToken(Item item) {
        return getMetadataValue(item, "cris.orcid.access-token")
            .map(metadataValue -> metadataValue.getValue());
    }

    public Optional<String> getOrcid(Item item) {
        return getMetadataValue(item, "person.identifier.orcid")
            .map(metadataValue -> metadataValue.getValue());
    }

    private Optional<MetadataValue> getMetadataValue(Item item, String metadataField) {
        return getMetadataValues(item, metadataField).findFirst();
    }

    private Stream<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return item.getMetadata().stream()
            .filter(metadata -> metadataField.equals(metadata.getMetadataField().toString('.')));
    }

}
