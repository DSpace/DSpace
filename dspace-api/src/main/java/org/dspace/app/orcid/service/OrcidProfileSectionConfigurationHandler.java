/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.util.List;

import org.dspace.app.orcid.model.OrcidProfileSectionConfiguration;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;

/**
 * Interface that mark classes that handle the configured instance of
 * {@link OrcidProfileSectionConfiguration}.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidProfileSectionConfigurationHandler {

    /**
     * Returns all the profile section configurations of the given type.
     *
     * @param  type the type of the section configurations to retrieve
     * @return      the section configurations of the given type
     */
    List<OrcidProfileSectionConfiguration> findBySectionType(OrcidProfileSectionType type);

    /**
     * Returns all the profile section configurations relative to the given
     * preferences.
     *
     * @param  preferences the preferences to search for
     * @return             the section configurations
     */
    List<OrcidProfileSectionConfiguration> findByPreferences(List<OrcidProfileSyncPreference> preferences);
}
