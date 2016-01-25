/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.dao.HarvestedItemDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;

/**
 * Hibernate implementation of the Database Access Object interface class for the HarvestedItem object.
 * This class is responsible for all database calls for the HarvestedItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedItemDAOImpl extends AbstractHibernateDAO<HarvestedItem> implements HarvestedItemDAO
{
    protected HarvestedItemDAOImpl()
    {
        super();
    }

    @Override
    public HarvestedItem findByItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedItem.class);
        criteria.add(Restrictions.eq("item", item));
        return singleResult(criteria);
    }

    @Override
    public HarvestedItem findByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedItem.class);
        criteria.createAlias("item", "i");
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("i.owningCollection", collection)
                )
        );
        return singleResult(criteria);
    }
}
