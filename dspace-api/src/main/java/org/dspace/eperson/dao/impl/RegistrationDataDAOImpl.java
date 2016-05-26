/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;

/**
 * Hibernate implementation of the Database Access Object interface class for the RegistrationData object.
 * This class is responsible for all database calls for the RegistrationData object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RegistrationDataDAOImpl extends AbstractHibernateDAO<RegistrationData> implements RegistrationDataDAO
{

    protected RegistrationDataDAOImpl()
    {
        super();
    }

    @Override
    public RegistrationData findByEmail(Context context, String email) throws SQLException {
        Criteria criteria = createCriteria(context, RegistrationData.class);
        criteria.add(Restrictions.eq("email", email));
        return uniqueResult(criteria);
    }

    @Override
    public RegistrationData findByToken(Context context, String token) throws SQLException {
        Criteria criteria = createCriteria(context, RegistrationData.class);
        criteria.add(Restrictions.eq("token", token));
        return uniqueResult(criteria);
    }

    @Override
    public void deleteByToken(Context context, String token) throws SQLException {
        String hql = "delete from RegistrationData where token=:token";
        Query query = createQuery(context, hql);
        query.setParameter("token", token);
        query.executeUpdate();
    }
}
