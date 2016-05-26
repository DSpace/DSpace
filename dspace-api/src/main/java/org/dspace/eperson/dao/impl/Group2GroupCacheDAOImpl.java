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
import org.dspace.eperson.Group;
import org.dspace.eperson.Group2GroupCache;
import org.dspace.eperson.dao.Group2GroupCacheDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Hibernate implementation of the Database Access Object interface class for the Group2GroupCache object.
 * This class is responsible for all database calls for the Group2GroupCache object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class Group2GroupCacheDAOImpl extends AbstractHibernateDAO<Group2GroupCache> implements Group2GroupCacheDAO
{
    protected Group2GroupCacheDAOImpl()
    {
        super();
    }

    @Override
    public List<Group2GroupCache> findByParent(Context context, Group group) throws SQLException {
        Criteria criteria = createCriteria(context, Group2GroupCache.class);
        criteria.add(Restrictions.eq("parent.id", group.getID()));
        criteria.setCacheable(true);

        return list(criteria);
    }

    @Override
    public List<Group2GroupCache> findByChildren(Context context, Set<Group> groups) throws SQLException {
        Criteria criteria = createCriteria(context, Group2GroupCache.class);

        Disjunction orDisjunction = Restrictions.or();
        for(Group group : groups)
        {
            orDisjunction.add(Restrictions.eq("child.id", group.getID()));
        }

        criteria.add(orDisjunction);
        criteria.setCacheable(true);

        return list(criteria);
    }

    @Override
    public Group2GroupCache find(Context context, Group parent, Group child) throws SQLException {
        Criteria criteria = createCriteria(context, Group2GroupCache.class);
        criteria.add(Restrictions.eq("parent.id", parent.getID()));
        criteria.add(Restrictions.eq("child.id", child.getID()));
        criteria.setCacheable(true);
        return uniqueResult(criteria);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        createQuery(context, "delete from Group2GroupCache").executeUpdate();
    }
}
