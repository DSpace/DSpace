/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.dao.impl;

import java.sql.SQLException;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.dao.OrcidTokenDAO;

/**
 * Implementation of {@link OrcidTokenDAO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidTokenDAOImpl extends AbstractHibernateDAO<OrcidToken> implements OrcidTokenDAO {

    @Override
    public OrcidToken findByEPerson(Context context, EPerson ePerson) {
        try {
            Query query = createQuery(context, "FROM OrcidToken WHERE ePerson = :ePerson");
            query.setParameter("ePerson", ePerson);
            return singleResult(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OrcidToken findByProfileItem(Context context, Item profileItem) {
        try {
            Query query = createQuery(context, "FROM OrcidToken WHERE profileItem = :profileItem");
            query.setParameter("profileItem", profileItem);
            return singleResult(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
