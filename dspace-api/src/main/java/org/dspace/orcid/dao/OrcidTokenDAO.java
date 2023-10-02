/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.dao;

import org.dspace.content.Item;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidToken;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the OrcidToken object. The
 * implementation of this class is responsible for all database calls for the
 * OrcidToken object and is autowired by spring. This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidTokenDAO extends GenericDAO<OrcidToken> {

    /**
     * Find an OrcidToken by ePerson.
     *
     * @param  session The current request's database context.
     * @param  ePerson the ePerson to search for
     * @return         the Orcid token, if any
     */
    public OrcidToken findByEPerson(Session session, EPerson ePerson);

    /**
     * Find an OrcidToken by profileItem.
     *
     * @param  session     The current request's database context.
     * @param  profileItem the profile item to search for
     * @return             the Orcid token, if any
     */
    public OrcidToken findByProfileItem(Session session, Item profileItem);

}
