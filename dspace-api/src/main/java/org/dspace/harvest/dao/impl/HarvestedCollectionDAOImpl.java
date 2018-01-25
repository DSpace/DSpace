/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import org.dspace.content.Bitstream;
import org.dspace.content.Bitstream_;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.HarvestedCollection_;
import org.dspace.harvest.dao.HarvestedCollectionDAO;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
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


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested)));
        criteriaQuery.orderBy(orderList);

        return singleResult(context, criteriaQuery);
    }

    @Override
    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedAsc(Context context, int status, int type, int limit) throws SQLException {
//        Old query: "select collection_id from harvested_collection where harvest_type > ? and harvest_status = ? order by last_harvested asc limit 1";

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested)));
        criteriaQuery.orderBy(orderList);

        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<HarvestedCollection> findByStatus(Context context, int status) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);
        criteriaQuery.where(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status));
        return list(context, criteriaQuery, false, HarvestedCollection.class, -1, -1);
    }

    @Override
    public HarvestedCollection findByCollection(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);
        criteriaQuery.where(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.collection), collection));
        return singleResult(context, criteriaQuery);

    }

    @Override
    public List<HarvestedCollection> findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(Context context, Date startTime, int minimalType, int[] statuses, int expirationStatus, Date expirationTime) throws SQLException {
//      Old query: "SELECT * FROM harvested_collection WHERE
// (last_harvested < ? or last_harvested is null) and harvest_type > ? and (harvest_status = ? or harvest_status = ? or (harvest_status=? and harvest_start_time < ?)) ORDER BY last_harvested",
//                new java.sql.Timestamp(startTime.getTime()), 0, HarvestedCollection.STATUS_READY, HarvestedCollection.STATUS_OAI_ERROR, HarvestedCollection.STATUS_BUSY, new java.sql.Timestamp(expirationTime.getTime()));


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);

        Predicate orPredicate = criteriaBuilder.or(criteriaBuilder.lessThan(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested), startTime),
                                                    criteriaBuilder.isNull(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested))
                                                    );

        List<Predicate> orPredicates = new LinkedList<>();

        for(int status : statuses){
            orPredicates.add(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status));
        }

        Predicate andPredicate = criteriaBuilder.and(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), expirationStatus),
                                                        criteriaBuilder.greaterThan(harvestedCollectionRoot.get(HarvestedCollection_.harvestStartTime), expirationTime)
                                                    );

        orPredicates.add(andPredicate);

        Predicate secondPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[]{}));

        criteriaQuery.where(criteriaBuilder.and(orPredicate,
                                                criteriaBuilder.greaterThan(harvestedCollectionRoot.get(HarvestedCollection_.harvestType), minimalType),
                                                secondPredicate
                                                )
                            );

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, HarvestedCollection.class, -1, -1);



    }

    @Override
    public int count(Context context) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        return count(context, criteriaQuery, criteriaBuilder, harvestedCollectionRoot);
    }

    protected CriteriaQuery getByStatusAndMinimalTypeCriteria(Context context, int status, int type, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.greaterThan(harvestedCollectionRoot.get(HarvestedCollection_.harvestType), type),
                                                criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status)
                                                )
                            );
        return criteriaQuery;
    }

}
