/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import java.util.Arrays;

/**
 * The entity types of the ORCID objects that can be synchronized.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidEntityType {

    PUBLICATION("Publication", "/work"),
    FUNDING("Project", "/funding");

    private final String entityType;

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

    public static boolean isValidEntityType(String entityType) {
        return Arrays.stream(OrcidEntityType.values())
            .anyMatch(orcidEntityType -> orcidEntityType.getEntityType().equalsIgnoreCase(entityType));
    }

    public static OrcidEntityType fromEntityType(String entityType) {
        return Arrays.stream(OrcidEntityType.values())
            .filter(orcidEntityType -> orcidEntityType.getEntityType().equalsIgnoreCase(entityType))
            .findFirst()
            .orElse(null);
    }
}
