/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.profile.OrcidEntitySynchronizationPreference;
import org.dspace.app.profile.OrcidProfileSynchronizationPreference;
import org.dspace.app.profile.OrcidSynchronizationMode;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;

/**
 * Service interface class for the {@link ResearcherProfile} object. The
 * implementation of this class is responsible for all business logic calls for
 * the {@link ResearcherProfile} object.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ResearcherProfileService {

    /**
     * Find the ResearcherProfile by UUID.
     *
     * @param context the relevant DSpace Context.
     * @param id      the ResearcherProfile id
     * @return the found ResearcherProfile
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ResearcherProfile findById(Context context, UUID id) throws SQLException, AuthorizeException;

    /**
     * Create a new researcher profile for the given ePerson.
     *
     * @param context the relevant DSpace Context.
     * @param ePerson the ePerson
     * @return the created profile
     * @throws SQLException
     * @throws AuthorizeException
     * @throws SearchServiceException
     */
    public ResearcherProfile createAndReturn(Context context, EPerson ePerson)
        throws AuthorizeException, SQLException, SearchServiceException;

    /**
     * Removes the association between the researcher profile and eperson related to
     * the input uuid.
     *
     * @param context the relevant DSpace Context.
     * @param id      the researcher profile id
     * @throws AuthorizeException
     * @throws SQLException
     */
    public void deleteById(Context context, UUID id) throws SQLException, AuthorizeException;

    /**
     * Changes the visibility of the given profile using the given new visible value
     *
     * @param context the relevant DSpace Context.
     * @param profile the researcher profile to update
     * @param visible the visible value to set
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void changeVisibility(Context context, ResearcherProfile profile, boolean visible)
        throws AuthorizeException, SQLException;

    /**
     * Update the ORCID synchronization preference related to the publications
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void updatePreferenceForSynchronizingPublicationsWithOrcid(Context context,
        ResearcherProfile researcherProfile, OrcidEntitySynchronizationPreference value) throws SQLException;

    /**
     * Update the ORCID synchronization preference related to the projects.
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void updatePreferenceForSynchronizingProjectsWithOrcid(Context context,
        ResearcherProfile researcherProfile, OrcidEntitySynchronizationPreference value) throws SQLException;

    /**
     * Update the ORCID synchronization preference related to the profile itself.
     *
     * @param  context                  the relevant DSpace Context.
     * @param  profile                  the researcher profile to update
     * @param  value                    the new synchronization preference value
     * @throws SQLException             if a SQL error occurs during the profile
     *                                  update
     * @throws IllegalArgumentException if the given researcher profile is no linked
     *                                  with an ORCID account
     */
    public void updatePreferenceForSynchronizingProfileWithOrcid(Context context,
        ResearcherProfile researcherProfile, List<OrcidProfileSynchronizationPreference> values) throws SQLException;

    /**
     * Update the ORCID synchronization mode.
     *
     * @param  context      the relevant DSpace Context.
     * @param  profile      the researcher profile to update
     * @param  value        the new synchronization mode value
     * @throws SQLException if a SQL error occurs during the profile update
     */
    public void updateOrcidSynchronizationMode(Context context, ResearcherProfile researcherProfile,
        OrcidSynchronizationMode value) throws SQLException;
}
