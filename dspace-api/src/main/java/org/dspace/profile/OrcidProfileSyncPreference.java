/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile;

/**
 * Enum that model the allowed values to configure the ORCID synchronization
 * preferences for the user's profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidProfileSyncPreference {

    /**
     * Data relating to the name, country and keywords of the ORCID profile.
     */
    BIOGRAPHICAL,

    /**
     * Data relating to external identifiers and researcher urls of the ORCID
     * profile.
     */
    IDENTIFIERS;
}
