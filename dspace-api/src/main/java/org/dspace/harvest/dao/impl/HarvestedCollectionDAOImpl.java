/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.HarvestedCollection_;
import org.dspace.harvest.dao.HarvestedCollectionDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the HarvestedCollection object.
 * This class is responsible for all database calls for the HarvestedCollection object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedCollectionDAOImpl extends AbstractHibernateDAO<HarvestedCollection>
    implements HarvestedCollectionDAO {
    protected HarvestedCollectionDAOImpl() {
        super();
    }


    @Override
    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedDesc(Context context, int status, int type,
                                                                                  int limit) throws SQLException {

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
    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedAsc(Context context, int status, int type,
                                                                                 int limit) throws SQLException {
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
        criteriaQuery
            .where(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status));
        return list(context, criteriaQuery, false, HarvestedCollection.class, -1, -1);
    }

    @Override
    public HarvestedCollection findByCollection(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);
        criteriaQuery
            .where(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.collection), collection));
        return singleResult(context, criteriaQuery);

    }

    @Override
    public List<HarvestedCollection>
        findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(Context context,
                                                                           Date startTime,
                                                                           int minimalType,
                                                                           int[] statuses,
                                                                           int expirationStatus,
                                                                           Date expirationTime)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);

        Predicate wasNotHarvestedInCurrentRun = criteriaBuilder
            .or(criteriaBuilder.lessThan(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested), startTime),
                criteriaBuilder.isNull(harvestedCollectionRoot.get(HarvestedCollection_.lastHarvested))
            );

        List<Predicate> hasCorrectStatusOrIsExpiredRestrictions = new LinkedList<>();

        for (int status : statuses) {
            hasCorrectStatusOrIsExpiredRestrictions
                .add(criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status));
        }

        Predicate harvestExpiredRestriction = criteriaBuilder.and(
            criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), expirationStatus),
            criteriaBuilder
                .greaterThan(harvestedCollectionRoot.get(HarvestedCollection_.harvestStartTime), expirationTime)
        );

        hasCorrectStatusOrIsExpiredRestrictions.add(harvestExpiredRestriction);

        Predicate hasCorrectStatusOrIsExpiredPredicate = criteriaBuilder.or(hasCorrectStatusOrIsExpiredRestrictions
                                                                                .toArray(new Predicate[] {}));

        Predicate hasMinimalType = criteriaBuilder.greaterThan(
            harvestedCollectionRoot.get(HarvestedCollection_.harvestType),
            minimalType);

        criteriaQuery.where(criteriaBuilder.and(wasNotHarvestedInCurrentRun,
                                                hasMinimalType,
                                                hasCorrectStatusOrIsExpiredPredicate
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

    protected CriteriaQuery getByStatusAndMinimalTypeCriteria(Context context, int status, int type)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, HarvestedCollection.class);
        Root<HarvestedCollection> harvestedCollectionRoot = criteriaQuery.from(HarvestedCollection.class);
        criteriaQuery.select(harvestedCollectionRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.greaterThan(harvestedCollectionRoot.get(HarvestedCollection_.harvestType), type),
            criteriaBuilder.equal(harvestedCollectionRoot.get(HarvestedCollection_.harvestStatus), status)
                            )
        );
        return criteriaQuery;
    }

}
