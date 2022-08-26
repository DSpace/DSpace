/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.List.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.content.Item.ANY;
import static org.dspace.profile.OrcidEntitySyncPreference.DISABLED;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.profile.OrcidEntitySyncPreference;
import org.dspace.profile.OrcidProfileDisconnectionMode;
import org.dspace.profile.OrcidProfileSyncPreference;
import org.dspace.profile.OrcidSynchronizationMode;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
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

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private OrcidTokenService orcidTokenService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public void linkProfile(Context context, Item profile, OrcidTokenResponseDTO token) throws SQLException {

        EPerson ePerson = ePersonService.findByProfileItem(context, profile);
        if (ePerson == null) {
            throw new IllegalArgumentException(
                "The given profile item is not related to any eperson. Item id: " + profile.getID());
        }

        String orcid = token.getOrcid();
        String accessToken = token.getAccessToken();
        String[] scopes = token.getScopeAsArray();

        itemService.setMetadataSingleValue(context, profile, "person", "identifier", "orcid", null, orcid);
        itemService.clearMetadata(context, profile, "dspace", "orcid", "scope", Item.ANY);
        for (String scope : scopes) {
            itemService.addMetadata(context, profile, "dspace", "orcid", "scope", null, scope);
        }

        if (isBlank(itemService.getMetadataFirstValue(profile, "dspace", "orcid", "authenticated", Item.ANY))) {
            String currentDate = ISO_DATE_TIME.format(now());
            itemService.setMetadataSingleValue(context, profile, "dspace", "orcid", "authenticated", null, currentDate);
        }

        setAccessToken(context, profile, ePerson, accessToken);

        EPerson ePersonByOrcid = ePersonService.findByNetid(context, orcid);
        if (ePersonByOrcid == null && isBlank(ePerson.getNetid())) {
            ePerson.setNetid(orcid);
            updateEPerson(context, ePerson);
        }

        updateItem(context, profile);

    }

    @Override
    public void unlinkProfile(Context context, Item profile) throws SQLException {

        itemService.clearMetadata(context, profile, "person", "identifier", "orcid", Item.ANY);
        itemService.clearMetadata(context, profile, "dspace", "orcid", "scope", Item.ANY);
        itemService.clearMetadata(context, profile, "dspace", "orcid", "authenticated", Item.ANY);

        orcidTokenService.deleteByProfileItem(context, profile);

        updateItem(context, profile);

    }

    @Override
    public boolean setEntityPreference(Context context, Item profile, OrcidEntityType type,
                                       OrcidEntitySyncPreference value) throws SQLException {
        String metadataQualifier = "sync-" + type.name().toLowerCase() + "s";
        return updatePreferenceForSynchronizingWithOrcid(context, profile, metadataQualifier, of(value.name()));
    }

    @Override
    public boolean setProfilePreference(Context context, Item profile, List<OrcidProfileSyncPreference> values)
        throws SQLException {

        List<String> valuesAsString = values.stream()
                                            .map(OrcidProfileSyncPreference::name)
                                            .collect(Collectors.toList());

        return updatePreferenceForSynchronizingWithOrcid(context, profile, "sync-profile", valuesAsString);

    }

    @Override
    public boolean setSynchronizationMode(Context context, Item profile, OrcidSynchronizationMode value)
        throws SQLException {

        if (!isLinkedToOrcid(context, profile)) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                                                   + "synchronization because it is not linked to any ORCID account: "
                                                   + profile.getID());
        }

        String newValue = value.name();
        String oldValue = itemService.getMetadataFirstValue(profile, "dspace", "orcid", "sync-mode", Item.ANY);

        if (StringUtils.equals(oldValue, newValue)) {
            return false;
        } else {
            itemService.setMetadataSingleValue(context, profile, "dspace", "orcid", "sync-mode", null, value.name());
            return true;
        }

    }

    @Override
    public boolean isSynchronizationAllowed(Item profile, Item item) {

        if (isOrcidSynchronizationDisabled()) {
            return false;
        }

        String entityType = itemService.getEntityTypeLabel(item);
        if (entityType == null) {
            return false;
        }

        if (OrcidEntityType.isValidEntityType(entityType)) {
            return getEntityPreference(profile, OrcidEntityType.fromEntityType(entityType))
                .filter(pref -> pref != DISABLED)
                .isPresent();
        }

        if (entityType.equals(researcherProfileService.getProfileType())) {
            return profile.equals(item) && !isEmpty(getProfilePreferences(profile));
        }

        return false;

    }

    @Override
    public Optional<OrcidSynchronizationMode> getSynchronizationMode(Item item) {
        return getMetadataValue(item, "dspace.orcid.sync-mode")
            .map(metadataValue -> metadataValue.getValue())
            .filter(value -> isValidEnum(OrcidSynchronizationMode.class, value))
            .map(value -> OrcidSynchronizationMode.valueOf(value));
    }

    @Override
    public Optional<OrcidEntitySyncPreference> getEntityPreference(Item item, OrcidEntityType entityType) {
        return getMetadataValue(item, "dspace.orcid.sync-" + entityType.name().toLowerCase() + "s")
            .map(metadataValue -> metadataValue.getValue())
            .filter(value -> isValidEnum(OrcidEntitySyncPreference.class, value))
            .map(value -> OrcidEntitySyncPreference.valueOf(value));
    }

    @Override
    public List<OrcidProfileSyncPreference> getProfilePreferences(Item item) {
        return getMetadataValues(item, "dspace.orcid.sync-profile")
            .map(MetadataValue::getValue)
            .filter(value -> isValidEnum(OrcidProfileSyncPreference.class, value))
            .map(value -> OrcidProfileSyncPreference.valueOf(value))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isLinkedToOrcid(Context context, Item item) {
        return getOrcidAccessToken(context, item).isPresent() && getOrcid(item).isPresent();
    }

    @Override
    public OrcidProfileDisconnectionMode getDisconnectionMode() {
        String value = configurationService.getProperty("orcid.disconnection.allowed-users");
        if (!OrcidProfileDisconnectionMode.isValid(value)) {
            return OrcidProfileDisconnectionMode.DISABLED;
        }
        return OrcidProfileDisconnectionMode.fromString(value);
    }

    private void setAccessToken(Context context, Item profile, EPerson ePerson, String accessToken) {
        OrcidToken orcidToken = orcidTokenService.findByEPerson(context, ePerson);
        if (orcidToken == null) {
            orcidTokenService.create(context, ePerson, profile, accessToken);
        } else {
            orcidToken.setProfileItem(profile);
            orcidToken.setAccessToken(accessToken);
        }
    }

    private boolean updatePreferenceForSynchronizingWithOrcid(Context context, Item profile,
                                                              String metadataQualifier,
                                                              List<String> values) throws SQLException {

        if (!isLinkedToOrcid(context, profile)) {
            throw new IllegalArgumentException("The given profile cannot be configured for the ORCID "
                                                   + "synchronization because it is not linked to any ORCID account: "
                                                   + profile.getID());
        }

        List<String> oldValues = itemService.getMetadata(profile, "dspace", "orcid", metadataQualifier, ANY).stream()
                                            .map(metadataValue -> metadataValue.getValue())
                                            .collect(Collectors.toList());

        if (containsSameValues(oldValues, values)) {
            return false;
        }

        itemService.clearMetadata(context, profile, "dspace", "orcid", metadataQualifier, ANY);
        for (String value : values) {
            itemService.addMetadata(context, profile, "dspace", "orcid", metadataQualifier, null, value);
        }

        return true;

    }

    private boolean containsSameValues(List<String> firstList, List<String> secondList) {
        return new HashSet<>(firstList).equals(new HashSet<>(secondList));
    }

    private Optional<String> getOrcidAccessToken(Context context, Item item) {
        return ofNullable(orcidTokenService.findByProfileItem(context, item))
            .map(orcidToken -> orcidToken.getAccessToken());
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


    private boolean isOrcidSynchronizationDisabled() {
        return !configurationService.getBooleanProperty("orcid.synchronization-enabled", true);
    }

    private void updateItem(Context context, Item item) throws SQLException {
        try {
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void updateEPerson(Context context, EPerson ePerson) throws SQLException {
        try {
            ePersonService.update(context, ePerson);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Item> findProfilesByOrcid(Context context, String orcid) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("search.entitytype:" + researcherProfileService.getProfileType());
        discoverQuery.addFilterQueries("person.identifier.orcid:" + orcid);
        try {
            return searchService.search(context, discoverQuery).getIndexableObjects().stream()
                .map(object -> ((IndexableItem) object).getIndexedObject())
                .collect(Collectors.toList());
        } catch (SearchServiceException ex) {
            throw new RuntimeException(ex);
        }
    }
}
