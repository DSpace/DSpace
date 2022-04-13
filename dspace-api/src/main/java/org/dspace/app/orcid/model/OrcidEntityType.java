/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import org.apache.commons.lang3.EnumUtils;

/**
 * The entity types of the ORCID objects that can be synchronized.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidEntityType {

    PUBLICATION("/work"),
    FUNDING("/funding");

    private final String path;

    private OrcidEntityType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static boolean isValid(String entityType) {
        return entityType != null ? EnumUtils.isValidEnum(OrcidEntityType.class, entityType.toUpperCase()) : false;
    }

    public static OrcidEntityType fromString(String entityType) {
        return isValid(entityType) ? OrcidEntityType.valueOf(entityType.toUpperCase()) : null;
    }
}
