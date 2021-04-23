/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

/**
 * Enum that model all the ORCID profile sections that could be synchronized.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidProfileSectionType {

    AFFILIATION,
    EDUCATION,
    QUALIFICATION,
    OTHER_NAMES,
    COUNTRY,
    KEYWORDS,
    EXTERNAL_IDS,
    RESEARCHER_URLS;
}
