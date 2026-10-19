/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.ArrayList;
import java.util.List;

/**
 * Small Spring-configurable relationship definition for the persistence POC.
 * A stable key identifies semantics even when no metadata projection exists.
 * Cardinality and richer lifecycle/projection policies remain separate follow-up work.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class RelationshipTypeConfiguration {
    private String id;
    private String leftMetadataField;
    private String rightMetadataField;
    private List<String> leftEntityTypes = new ArrayList<>();
    private List<String> rightEntityTypes = new ArrayList<>();

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

    public String getLeftMetadataField() {
        return leftMetadataField;
    }

    public void setLeftMetadataField(String field) {
        this.leftMetadataField = field;
    }

    public String getRightMetadataField() {
        return rightMetadataField;
    }

    public void setRightMetadataField(String field) {
        this.rightMetadataField = field;
    }

    public List<String> getLeftEntityTypes() {
        return List.copyOf(leftEntityTypes);
    }

    public void setLeftEntityTypes(List<String> types) {
        leftEntityTypes = new ArrayList<>(types);
    }

    public List<String> getRightEntityTypes() {
        return List.copyOf(rightEntityTypes);
    }

    public void setRightEntityTypes(List<String> types) {
        rightEntityTypes = new ArrayList<>(types);
    }

    /** Resolve the orientation of an anchor field. Ambiguous mappings fail explicitly. */
    public boolean isOwnerOnLeft(String field) {
        boolean left = field.equals(leftMetadataField);
        boolean right = field.equals(rightMetadataField);
        if (left == right) {
            throw new IllegalArgumentException("Field must match exactly one side of relationship configuration " + id);
        }
        return left;
    }

    public boolean mapsField(String field) {
        return field.equals(leftMetadataField) || field.equals(rightMetadataField);
    }
}
