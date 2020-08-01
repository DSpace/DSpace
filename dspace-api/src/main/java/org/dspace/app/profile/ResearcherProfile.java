/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.util.UUID;

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

    public Item getItem() {
        return item;
    }

    private MetadataValue getCrisOwnerMetadata(Item item) {
        return item.getMetadata().stream()
            .filter(metadata -> "cris.owner".equals(metadata.getMetadataField().toString('.')))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("A profile item must have a cris.owner metadata"));
    }

}
