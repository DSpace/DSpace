/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidToken;

/**
 * Service that handle {@link OrcidToken} entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidTokenService {

    /**
     * Creates a new OrcidToken entity for the given ePerson and accessToken.
     *
     * @param  context     the DSpace context
     * @param  ePerson     the EPerson
     * @param  accessToken the access token
     * @return             the created entity instance
     */
    public OrcidToken create(Context context, EPerson ePerson, String accessToken);

    /**
     * Creates a new OrcidToken entity for the given ePerson and accessToken.
     *
     * @param  context     the DSpace context
     * @param  ePerson     the EPerson
     * @param  profileItem the profile item
     * @param  accessToken the access token
     * @return             the created entity instance
     */
    public OrcidToken create(Context context, EPerson ePerson, Item profileItem, String accessToken);

    /**
     * Find an OrcidToken by ePerson.
     *
     * @param  context the DSpace context
     * @param  ePerson the ePerson to search for
     * @return         the Orcid token, if any
     */
    public OrcidToken findByEPerson(Context context, EPerson ePerson);

    /**
     * Find an OrcidToken by profileItem.
     *
     * @param  context     the DSpace context
     * @param  profileItem the profile item to search for
     * @return             the Orcid token, if any
     */
    public OrcidToken findByProfileItem(Context context, Item profileItem);

    /**
     * Delete the given ORCID token entity.
     *
     * @param context    the DSpace context
     * @param orcidToken the ORCID token entity to delete
     */
    public void delete(Context context, OrcidToken orcidToken);

    /**
     * Delete all the ORCID token entities.
     *
     * @param context the DSpace context
     */
    public void deleteAll(Context context);

    /**
     * Deletes the ORCID token entity related to the given EPerson.
     *
     * @param context the DSpace context
     * @param ePerson the ePerson for the deletion
     */
    public void deleteByEPerson(Context context, EPerson ePerson);

    /**
     * Deletes the ORCID token entity related to the given profile item.
     *
     * @param context     the DSpace context
     * @param profileItem the item for the deletion
     */
    public void deleteByProfileItem(Context context, Item profileItem);
}
