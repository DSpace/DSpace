/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.dao.CollectionRoleDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the CollectionRole object.
 * This class is responsible for all database calls for the CollectionRole object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionRoleDAOImpl extends AbstractHibernateDAO<CollectionRole> implements CollectionRoleDAO
{
    protected CollectionRoleDAOImpl()
    {
        super();
    }

    @Override
    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, CollectionRole.class);
        criteria.add(Restrictions.eq("collection", collection));

        return list(criteria);
    }

    @Override
    public CollectionRole findByCollectionAndRole(Context context, Collection collection, String role) throws SQLException {
        Criteria criteria = createCriteria(context, CollectionRole.class);
        criteria.add(Restrictions.and(
                        Restrictions.eq("collection", collection),
                        Restrictions.eq("roleId", role)
                )
        );

        return uniqueResult(criteria);

    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        String hql = "delete from CollectionRole WHERE collection=:collection";
        Query query = createQuery(context, hql);
        query.setParameter("collection", collection);
        query.executeUpdate();
    }
}
