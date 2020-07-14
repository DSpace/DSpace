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
     * Create a new researcher profile with the given id and the given visibility.
     *
     * @param context the relevant DSpace Context.
     * @param id      the researcher profile id
     * @return the created profile
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ResearcherProfile createAndReturn(Context context, UUID id) throws AuthorizeException, SQLException;

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
}
