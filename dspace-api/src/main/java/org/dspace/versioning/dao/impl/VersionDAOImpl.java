/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao.impl;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.dao.VersionDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the Version object.
 * This class is responsible for all database calls for the Version object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author kevinvandevelde at atmire.com
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
public class VersionDAOImpl extends AbstractHibernateDAO<Version> implements VersionDAO
{
    protected VersionDAOImpl()
    {
        super();
    }

    @Override
    public Version findByItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, Version.class);
        criteria.add(Restrictions.eq("item", item));
        return singleResult(criteria);
    }

    @Override
    public int getNextVersionNumber(Context c, VersionHistory vh) throws SQLException {
        Query q = this.createQuery(c, 
                "SELECT (COALESCE(MAX(versionNumber), 0) + 1) "
                        + "FROM Version WHERE versionHistory.id = :historyId");
        q.setParameter("historyId", vh.getID());

        int next = (Integer) q.uniqueResult();
        return next;
    }
    
    @Override
    public List<Version> findVersionsWithItems(Context context, VersionHistory versionHistory)
            throws SQLException
    {
        Criteria criteria = createCriteria(context, Version.class);
        criteria.add(Restrictions.eq("versionHistory", versionHistory));
        criteria.add(Restrictions.and(Restrictions.isNotNull("item")));
        criteria.addOrder(Order.desc("versionNumber"));
        return list(criteria);
    }
}
