/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.profile.OrcidEntitySyncPreference;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.app.profile.OrcidSyncMode;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service that handle the the syncronization between a DSpace profile and the
 * relative ORCID profile, if any.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidSynchronizationService {

    /**
     * Check if the given item is linked to an ORCID profile.
     *
     * @param  item the item to check
     * @return      true if the given item is linked to ORCID
     */
    boolean isLinkedToOrcid(Item item);

    /**
     * Configure the given profile with the data present in the given ORCID token.
     * This action is required to synchronize profile and related entities with
     * ORCID.
     *
     * @param  context      the relevant DSpace Context.
     * @param  profile      the profile to configure
     * @param  token        the ORCID token
     * @throws SQLException if a SQL error occurs during the profile update
     */
    public void linkProfile(Context context, Item profile, OrcidTokenResponseDTO token) throws SQLException;

    /**
     * Set the publications synchronization preference for the given profile.
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void setPublicationPreference(Context context, Item profile, OrcidEntitySyncPreference value)
        throws SQLException;

    /**
     * Set the projects synchronization preference for the given profile.
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void setProjectPreference(Context context, Item profile, OrcidEntitySyncPreference value)
        throws SQLException;

    /**
     * Update the profile's synchronization preference for the given profile.
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void setProfilePreference(Context context, Item profile,
        List<OrcidProfileSyncPreference> values) throws SQLException;

    /**
     * Set the ORCID synchronization mode for the given profile.
     *
     * @param  context      the relevant DSpace Context.
     * @param  profile      the researcher profile to update
     * @param  value        the new synchronization mode value
     * @throws SQLException if a SQL error occurs during the profile update
     */
    public void setSynchronizationMode(Context context, Item profile, OrcidSyncMode value) throws SQLException;

    /**
     * Check if the given researcher profile item is configured to synchronize the
     * given item with ORCID.
     *
     * @param  profile the researcher profile item
     * @param  item    the entity type to check
     * @return         true if the given entity type can be synchronize with ORCID,
     *                 false otherwise
     */
    public boolean isSynchronizationEnabled(Item profile, Item item);

    /**
     * Returns the ORCID synchronization mode configured for the given profile item.
     *
     * @param  profile the researcher profile item
     * @return         the synchronization mode
     */
    Optional<OrcidSyncMode> getSynchronizationMode(Item profile);

    /**
     * Returns the ORCID synchronization preference related to publications
     * configured for the given profile item.
     *
     * @param  profile the researcher profile item
     * @return         the configured preference
     */
    Optional<OrcidEntitySyncPreference> getPublicationsPreference(Item profile);

    /**
     * Returns the ORCID synchronization preference related to projects configured
     * for the given profile item.
     *
     * @param  profile the researcher profile item
     * @return         the synchronization mode
     */
    Optional<OrcidEntitySyncPreference> getProjectsPreference(Item profile);

    /**
     * Returns the ORCID synchronization preferences related to the profile itself
     * configured for the given profile item.
     *
     * @param  profile the researcher profile item
     * @return         the synchronization mode
     */
    List<OrcidProfileSyncPreference> getProfilePreferences(Item profile);
}
