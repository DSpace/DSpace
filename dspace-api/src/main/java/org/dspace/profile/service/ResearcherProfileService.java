/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile.service;

import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;

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
     * @param  context                the relevant DSpace Context.
     * @param  ePerson                the ePerson
     * @return                        the created profile
     * @throws SQLException
     * @throws AuthorizeException
     * @throws SearchServiceException
     */
    public ResearcherProfile createAndReturn(Context context, EPerson ePerson)
        throws AuthorizeException, SQLException, SearchServiceException;

    /**
     * Delete the profile with the given id. Based on the
     * researcher-profile.hard-delete.enabled configuration, this method deletes the
     * related item or removes the association between the researcher profile and
     * eperson related to the input uuid.
     *
     * @param  context            the relevant DSpace Context.
     * @param  id                 the researcher profile id
     * @throws AuthorizeException
     * @throws SQLException
     */
    public void deleteById(Context context, UUID id) throws SQLException, AuthorizeException;

    /**
     * Changes the visibility of the given profile using the given new visible
     * value. The visiblity controls whether the Profile is Anonymous READ or not.
     *
     * @param  context            the relevant DSpace Context.
     * @param  profile            the researcher profile to update
     * @param  visible            the visible value to set. If true the profile will
     *                            be visible to all users.
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void changeVisibility(Context context, ResearcherProfile profile, boolean visible)
        throws AuthorizeException, SQLException;

    /**
     * Claims and links an eperson to an existing DSpaceObject
     * @param  context                  the relevant DSpace Context.
     * @param  ePerson                  the ePerson
     * @param  uri                      uri of existing Item to be linked to the
     *                                  eperson
     * @return                          the created profile
     * @throws IllegalArgumentException if the given uri is not related to an
     *                                  archived item or if the item cannot be
     *                                  claimed
     */
    ResearcherProfile claim(Context context, EPerson ePerson, URI uri)
        throws SQLException, AuthorizeException, SearchServiceException;

    /**
     * Check if the given item has an entity type compatible with that of the
     * researcher profile. If the given item does not have an entity type, the check
     * returns false.
     * 
     * @param  item the item to check
     * @return      the check result
     */
    boolean hasProfileType(Item item);

    /**
     * Returns the profile entity type, if any.
     *
     * @return the profile type
     */
    String getProfileType();
}
