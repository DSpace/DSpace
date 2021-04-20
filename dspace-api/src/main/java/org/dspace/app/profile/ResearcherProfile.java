/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static org.dspace.app.profile.OrcidEntitySynchronizationPreference.DISABLED;
import static org.dspace.app.profile.OrcidSynchronizationMode.MANUAL;
import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.util.UUIDUtils;
import org.springframework.util.Assert;

/**
 * Object representing a Researcher Profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfile {

    private final Item item;

    private final MetadataValue crisOwner;

    /**
     * Create a new ResearcherProfile object from the given item.
     *
     * @param  item                     the profile item
     * @throws IllegalArgumentException if the given item has not a cris.owner
     *                                  metadata with a valid authority
     */
    public ResearcherProfile(Item item) {
        Assert.notNull(item, "A researcher profile requires an item");
        this.item = item;
        this.crisOwner = getCrisOwnerMetadata(item);
    }

    public UUID getId() {
        return UUIDUtils.fromString(crisOwner.getAuthority());
    }

    public String getFullName() {
        return crisOwner.getValue();
    }

    public boolean isVisible() {
        return item.getResourcePolicies().stream()
            .filter(policy -> policy.getGroup() != null)
            .anyMatch(policy -> READ == policy.getAction() && ANONYMOUS.equals(policy.getGroup().getName()));
    }

    public boolean isLinkedToOrcid() {
        return getOrcidAccessToken().isPresent() && getOrcid().isPresent();
    }

    public Item getItem() {
        return item;
    }

    public Optional<String> getOrcid() {
        return getMetadataValue(item, "person.identifier.orcid")
            .map(metadataValue -> metadataValue.getValue());
    }

    public String getOrcidSynchronizationMode() {
        return getMetadataValue(item, "cris.orcid.sync-mode")
            .map(metadataValue -> metadataValue.getValue())
            .orElse(MANUAL.name());
    }

    public String getOrcidSynchronizationPublicationsPreference() {
        return getMetadataValue(item, "cris.orcid.sync-publications")
            .map(metadataValue -> metadataValue.getValue())
            .orElse(DISABLED.name());
    }

    public String getOrcidSynchronizationProjectsPreference() {
        return getMetadataValue(item, "cris.orcid.sync-projects")
            .map(metadataValue -> metadataValue.getValue())
            .orElse(DISABLED.name());
    }

    public List<String> getOrcidSynchronizationProfilePreferences() {
        return getMetadataValues(item, "cris.orcid.sync-profile")
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    private MetadataValue getCrisOwnerMetadata(Item item) {
        return getMetadataValue(item, "cris.owner")
            .filter(metadata -> UUIDUtils.fromString(metadata.getAuthority()) != null)
            .orElseThrow(() -> new IllegalArgumentException("A profile item must have a valid cris.owner metadata"));
    }

    private Optional<String> getOrcidAccessToken() {
        return getMetadataValue(item, "cris.orcid.access-token")
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
