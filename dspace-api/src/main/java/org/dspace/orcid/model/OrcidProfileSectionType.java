/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model;

import org.apache.commons.lang3.EnumUtils;

/**
 * Enum that model all the ORCID profile sections that could be synchronized.
 * These fields come from the ORCID PERSON schema, see
 * https://info.orcid.org/documentation/integration-guide/orcid-record/#PERSON
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidProfileSectionType {

    OTHER_NAMES("/other-names"),
    COUNTRY("/address"),
    KEYWORDS("/keywords"),
    EXTERNAL_IDS("/external-identifiers"),
    RESEARCHER_URLS("/researcher-urls");

    private final String path;

    private OrcidProfileSectionType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static boolean isValid(String type) {
        return type != null ? EnumUtils.isValidEnum(OrcidProfileSectionType.class, type.toUpperCase()) : false;
    }

    public static OrcidProfileSectionType fromString(String type) {
        return isValid(type) ? OrcidProfileSectionType.valueOf(type.toUpperCase()) : null;
    }

}
