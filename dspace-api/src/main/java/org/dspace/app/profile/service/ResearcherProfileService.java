/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.sql.SQLException;
import java.util.UUID;

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
}
