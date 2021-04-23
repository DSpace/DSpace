/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import static java.lang.String.format;

import java.util.List;

import org.dspace.app.profile.OrcidProfileSyncPreference;

/**
 * Interface to mark all the available orcid profile section configurations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class OrcidProfileSectionConfiguration {

    private final OrcidProfileSectionType sectionType;

    private final OrcidProfileSyncPreference preference;

    public OrcidProfileSectionConfiguration(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference) {
        this.sectionType = sectionType;
        this.preference = preference;

        if (!getSupportedTypes().contains(sectionType)) {
            throw new IllegalArgumentException(format("The ORCID configuration does not support "
                + "the section type %s. Supported types are %s", sectionType, getSupportedTypes()));
        }

    }

    /**
     * Returns the section type.
     *
     * @return the section type
     */
    public OrcidProfileSectionType getSectionType() {
        return sectionType;
    }

    /**
     * Returns the synchronization preference related to this configuration.
     *
     * @return the section name
     */
    public OrcidProfileSyncPreference getSynchronizationPreference() {
        return preference;
    }

    /**
     * Returns all the supported profile section types.
     *
     * @return the supported sections
     */
    public abstract List<OrcidProfileSectionType> getSupportedTypes();

    /**
     * Returns all the metadata fields involved in the profile section
     * configuration.
     *
     * @return the metadataFields
     */
    public abstract List<String> getMetadataFields();

}
