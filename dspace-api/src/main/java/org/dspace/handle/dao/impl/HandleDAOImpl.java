/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao.impl;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.handle.Handle;
import org.dspace.handle.dao.HandleDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the Handle object.
 * This class is responsible for all database calls for the Handle object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HandleDAOImpl extends AbstractHibernateDAO<Handle> implements HandleDAO {

    @Override
    public List<Handle> getHandlesByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("dso", dso)
        ));
        return list(criteria);
    }

    @Override
    public Handle findByHandle(Context context, String handle) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.eq("handle", handle));
        return uniqueResult(criteria);
    }

    @Override
    public List<Handle> findByPrefix(Context context, String prefix) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.like("handle", prefix + "%"));
        return list(criteria);
    }

    @Override
    public long countHandlesByPrefix(Context context, String prefix) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.like("handle", prefix + "%"));
        return countLong(criteria);
    }

    @Override
    public int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException
    {
        String hql = "UPDATE handle set handle = concat(:newPrefix,'/',id WHERE handle like :oldPrefix%";
        Query query = createQuery(context, hql);
        query.setString("newPrefix", newPrefix);
        query.setString("oldPrefix", oldPrefix);
        return query.executeUpdate();
    }
}
