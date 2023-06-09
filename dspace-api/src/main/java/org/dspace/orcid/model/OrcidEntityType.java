/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model;

import java.util.Arrays;

/**
 * The types of activities defined on ORCID that can be synchronized.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidEntityType {

    /**
     * The ORCID publication/work activity.
     */
    PUBLICATION("Publication", "/work"),

    /**
     * The ORCID funding activity.
     */
    FUNDING("Project", "/funding");

    /**
     * The DSpace entity type.
     */
    private final String entityType;

    /**
     * The subpath of the activity on ORCID API.
     */
    private final String path;

    private OrcidEntityType(String entityType, String path) {
        this.entityType = entityType;
        this.path = path;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getPath() {
        return path;
    }

    /**
     * Check if the given DSpace entity type is valid.
     * @param  entityType the entity type to check
     * @return            true if valid, false otherwise
     */
    public static boolean isValidEntityType(String entityType) {
        return Arrays.stream(OrcidEntityType.values())
            .anyMatch(orcidEntityType -> orcidEntityType.getEntityType().equalsIgnoreCase(entityType));
    }

    /**
     * Returns an ORCID entity type from a DSpace entity type.
     *
     * @param  entityType the DSpace entity type to search for
     * @return            the ORCID entity type, if any
     */
    public static OrcidEntityType fromEntityType(String entityType) {
        return Arrays.stream(OrcidEntityType.values())
            .filter(orcidEntityType -> orcidEntityType.getEntityType().equalsIgnoreCase(entityType))
            .findFirst()
            .orElse(null);
    }
}
