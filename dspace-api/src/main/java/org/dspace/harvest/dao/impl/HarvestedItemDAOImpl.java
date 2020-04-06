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
import java.util.List;

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
        criteria.add(Restrictions.eq("oaiId", itemOaiID));
        HarvestedItem harvestedItem = singleResult(criteria);
        boolean alreadyHarvestedToCollection = false;
        if (harvestedItem != null && harvestedItem.getItem() != null) {
            List<Collection> collections = harvestedItem.getItem().getCollections();
            if (collections != null && !collections.isEmpty()) {
                for (Collection collectionOfHarvestedItem : collections) {
                    if (collectionOfHarvestedItem.equals(collection)) {
                        alreadyHarvestedToCollection = true;
                        break;
                    }

                }
            }
        }
        return alreadyHarvestedToCollection ? harvestedItem : null;
    }
}
