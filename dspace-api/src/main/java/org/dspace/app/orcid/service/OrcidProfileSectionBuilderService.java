/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.util.List;

import org.dspace.app.orcid.builder.OrcidProfileSectionBuilder;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface that mark classes that handle the configured instance of
 * {@link OrcidProfileSectionBuilder}.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidProfileSectionBuilderService {

    /**
     * Returns all the profile section configurations of the given type.
     *
     * @param  type the type of the section configurations to retrieve
     * @return      the section configurations of the given type
     */
    List<OrcidProfileSectionBuilder> findBySectionType(OrcidProfileSectionType type);

    /**
     * Returns all the profile section configurations relative to the given
     * preferences.
     *
     * @param  preferences the preferences to search for
     * @return             the section configurations
     */
    List<OrcidProfileSectionBuilder> findByPreferences(List<OrcidProfileSyncPreference> preferences);

    /**
     * Builds many instance of ORCID objects starting from the given item compliance
     * to the given profile section type.
     *
     * @param  context the DSpace context
     * @param  item    the item
     * @param  type    the profile section type
     * @return         the created objects
     */
    List<Object> buildOrcidObjects(Context context, Item item, OrcidProfileSectionType type);

    /**
     * Get the metadata signature of the givn item's metadata values related to the
     * given profile section type.
     *
     * @param  context the DSpace context
     * @param  item    the item
     * @param  type    the type of the section configurations
     * @return         the metadata signature
     */
    String getMetadataSignature(Context context, Item item, OrcidProfileSectionType type);
}
