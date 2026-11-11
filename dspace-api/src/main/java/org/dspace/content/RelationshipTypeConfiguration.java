/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;

/**
 * Spring-configurable definition of a relationship between two Item entity types.
 *
 * Defines the primary metadata field on each endpoint and the generators
 * responsible for additional metadata associated with the relationship.
 * The configuration is identified by a stable key persisted on the Relationship.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class RelationshipTypeConfiguration {
    private String id;

    private String leftPrimaryMetadataField;
    private String rightPrimaryMetadataField;

    private Set<String> leftEntityTypes = new HashSet<>();
    private Set<String> rightEntityTypes = new HashSet<>();

    private Map<String, ItemAuthorityExtraMetadataGenerator> leftMetadataGenerators = new HashMap<>();
    private Map<String, ItemAuthorityExtraMetadataGenerator> rightMetadataGenerators = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank() || id.length() > 255) {
            throw new IllegalArgumentException(
                    "A non-empty relationship config key of at most 255 characters is required");
        }
        this.id = id;
    }

    public Optional<String> getLeftPrimaryMetadataField() {
        return Optional.ofNullable(leftPrimaryMetadataField);
    }

    public void setLeftPrimaryMetadataField(String field) {
        this.leftPrimaryMetadataField = field;
    }

    public Optional<String> getRightPrimaryMetadataField() {
        return Optional.ofNullable(rightPrimaryMetadataField);
    }

    public void setRightPrimaryMetadataField(String field) {
        this.rightPrimaryMetadataField = field;
    }

    public Set<String> getLeftEntityTypes() {
        return Set.copyOf(leftEntityTypes);
    }

    public void setLeftEntityTypes(Set<String> types) {
        this.leftEntityTypes = new HashSet<>(types);
    }

    public Set<String> getRightEntityTypes() {
        return Set.copyOf(rightEntityTypes);
    }

    public void setRightEntityTypes(Set<String> types) {
        this.rightEntityTypes = new HashSet<>(types);
    }

    /**
     * Resolve the orientation of an anchor field. Ambiguous mappings fail explicitly.
     */
    public boolean isOwnerOnLeft(String field) {
        boolean left = field.equals(leftPrimaryMetadataField);
        boolean right = field.equals(rightPrimaryMetadataField);
        if (left == right) {
            throw new IllegalArgumentException("Field must match exactly one side of relationship configuration " + id);
        }
        return left;
    }

    public boolean mapsField(String field) {
        return field.equals(leftPrimaryMetadataField) || field.equals(rightPrimaryMetadataField);
    }

    public Map<String, ItemAuthorityExtraMetadataGenerator> getLeftMetadataGenerators() {
        return Map.copyOf(leftMetadataGenerators);
    }

    public void setLeftMetadataGenerators(Map<String, ItemAuthorityExtraMetadataGenerator> leftMetadataGenerators) {
        if (leftMetadataGenerators.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Metadata generators cannot be null on the left endpoint");
        }

        this.leftMetadataGenerators = new HashMap<>(leftMetadataGenerators);
    }

    public Map<String, ItemAuthorityExtraMetadataGenerator> getRightMetadataGenerators() {
        return Map.copyOf(rightMetadataGenerators);
    }

    public void setRightMetadataGenerators(Map<String, ItemAuthorityExtraMetadataGenerator> rightMetadataGenerators) {
        if (rightMetadataGenerators.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Metadata generators cannot be null on the right endpoint");
        }

        this.rightMetadataGenerators = new HashMap<>(rightMetadataGenerators);
    }

    public Set<String> getLeftAdditionalMetadataFields() {
        return Set.copyOf(leftMetadataGenerators.keySet());
    }

    public Set<String> getRightAdditionalMetadataFields() {
        return Set.copyOf(rightMetadataGenerators.keySet());
    }
}
