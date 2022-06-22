/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import java.util.List;
import java.util.Optional;

import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.orcid.model.factory.OrcidProfileSectionFactory;
import org.dspace.profile.OrcidProfileSyncPreference;

/**
 * Interface that mark classes that handle the configured instance of
 * {@link OrcidProfileSectionFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidProfileSectionFactoryService {

    /**
     * Returns the profile section factory of the given type.
     *
     * @param  type the type of the section configurations to retrieve
     * @return      the section configurations of the given type
     */
    Optional<OrcidProfileSectionFactory> findBySectionType(OrcidProfileSectionType type);

    /**
     * Returns all the profile section configurations relative to the given
     * preferences.
     *
     * @param  preferences the preferences to search for
     * @return             the section configurations
     */
    List<OrcidProfileSectionFactory> findByPreferences(List<OrcidProfileSyncPreference> preferences);

    /**
     * Builds an ORCID object starting from the given metadata values compliance to
     * the given profile section type.
     *
     * @param  context        the DSpace context
     * @param  metadataValues the metadata values
     * @param  type           the profile section type
     * @return                the created object
     */
    Object createOrcidObject(Context context, List<MetadataValue> metadataValues, OrcidProfileSectionType type);
}
