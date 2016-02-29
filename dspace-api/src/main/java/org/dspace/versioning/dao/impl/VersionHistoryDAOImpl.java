/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao.impl;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.dao.VersionHistoryDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;
import org.dspace.versioning.Version;
import org.hibernate.criterion.Order;

/**
 * Hibernate implementation of the Database Access Object interface class for the VersionHistory object.
 * This class is responsible for all database calls for the VersionHistory object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author kevinvandevelde at atmire.com
 */
public class VersionHistoryDAOImpl extends AbstractHibernateDAO<VersionHistory> implements VersionHistoryDAO
{
    protected VersionHistoryDAOImpl()
    {
        super();
    }

    @Override
    public VersionHistory findByItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, VersionHistory.class);
        criteria.createAlias("versions", "v");
        criteria.add(Restrictions.eq("v.item", item));
        criteria.addOrder(Order.desc("v.versionNumber"));
        return singleResult(criteria);
    }
}
