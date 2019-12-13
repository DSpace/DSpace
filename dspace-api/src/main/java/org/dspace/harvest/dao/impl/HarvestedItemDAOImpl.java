/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import java.sql.SQLException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.HarvestedItem_;
import org.dspace.harvest.dao.HarvestedItemDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the HarvestedItem object.
 * This class is responsible for all database calls for the HarvestedItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedItemDAOImpl extends AbstractHibernateDAO<HarvestedItem> implements HarvestedItemDAO {
    protected HarvestedItemDAOImpl() {
        super();
    }

    @Override
    public HarvestedItem findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedItem.class);
        Root<HarvestedItem> harvestedItemRoot = criteriaQuery.from(HarvestedItem.class);
        criteriaQuery.select(harvestedItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(harvestedItemRoot.get(HarvestedItem_.item), item));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public HarvestedItem findByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedItem.class);
        Root<HarvestedItem> harvestedItemRoot = criteriaQuery.from(HarvestedItem.class);
        Join<HarvestedItem, Item> join = harvestedItemRoot.join("item");
        criteriaQuery.select(harvestedItemRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(harvestedItemRoot.get(HarvestedItem_.oaiId), itemOaiID),
                                       criteriaBuilder.equal(join.get(Item_.owningCollection), collection)
                   )
        );
        return singleResult(context, criteriaQuery);

    }
}
