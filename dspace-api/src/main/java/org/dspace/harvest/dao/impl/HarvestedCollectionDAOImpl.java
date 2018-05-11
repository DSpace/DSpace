/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.dao.HarvestedCollectionDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the HarvestedCollection object.
 * This class is responsible for all database calls for the HarvestedCollection object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedCollectionDAOImpl extends AbstractHibernateDAO<HarvestedCollection> implements HarvestedCollectionDAO
{
    protected HarvestedCollectionDAOImpl()
    {
        super();
    }


    @Override
    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedDesc(Context context, int status, int type, int limit) throws SQLException {
//      Old query: "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? order by last_harvested desc limit 1";
        Criteria criteria = getByStatusAndMinimalTypeCriteria(context, status, type, limit);
        criteria.addOrder(Order.desc("lastHarvested"));
        return singleResult(criteria);
    }

    @Override
    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedAsc(Context context, int status, int type, int limit) throws SQLException {
//        Old query: "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? order by last_harvested asc limit 1";
        Criteria criteria = getByStatusAndMinimalTypeCriteria(context, status, type, limit);
        criteria.addOrder(Order.asc("lastHarvested"));
        return singleResult(criteria);
    }

    @Override
    public List<HarvestedCollection> findByStatus(Context context, int status) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedCollection.class);
        criteria.add(Restrictions.eq("harvestStatus", status));
        return list(criteria);
    }

    @Override
    public HarvestedCollection findByCollection(Context context, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedCollection.class);
        criteria.add(Restrictions.eq("collection", collection));
        return singleResult(criteria);

    }

    @Override
    public List<HarvestedCollection> findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(Context context, Date startTime, int minimalType, int[] statuses, int expirationStatus, Date expirationTime) throws SQLException {
//      Old query: "SELECT * FROM harvested_collection WHERE
// (last_harvested < ? or last_harvested is null) and harvest_type > ? and (harvest_status = ? or harvest_status = ? or (harvest_status=? and harvest_start_time < ?)) ORDER BY last_harvested",
//                new java.sql.Timestamp(startTime.getTime()), 0, HarvestedCollection.STATUS_READY, HarvestedCollection.STATUS_OAI_ERROR, HarvestedCollection.STATUS_BUSY, new java.sql.Timestamp(expirationTime.getTime()));
        Criteria criteria = createCriteria(context, HarvestedCollection.class);
        LogicalExpression lastHarvestedRestriction = Restrictions.or(
                Restrictions.lt("lastHarvested", startTime),
                Restrictions.isNull("lastHarvested")
        );
        Disjunction statusRestriction = Restrictions.or();
        for (int status : statuses) {
            statusRestriction.add(Restrictions.eq("harvestStatus", status));
        }
        statusRestriction.add(
                Restrictions.and(
                        Restrictions.eq("harvestStatus", expirationStatus),
                        Restrictions.gt("harvestStartTime", expirationTime)
                )
        );

        criteria.add(
                Restrictions.and(
                        lastHarvestedRestriction,
                        Restrictions.gt("harvestType", minimalType),
                        statusRestriction

                )
        );
        criteria.addOrder(Order.asc("lastHarvested"));
        return list(criteria);

    }

    @Override
    public int count(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedCollection.class);
        return count(criteria);
    }

    protected Criteria getByStatusAndMinimalTypeCriteria(Context context, int status, int type, int limit) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedCollection.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.gt("harvestType", type),
                        Restrictions.eq("harvestStatus", status)
                )
        );
        if(limit != -1)
        {
            criteria.setMaxResults(1);
        }
        return criteria;
    }

}
