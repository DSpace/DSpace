/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.CrisMetrics_;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObject_;
import org.dspace.content.Item_;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the CrisMetrics object.
 * This class is responsible for all database calls for the CrisMetrics object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricsDAOImpl extends AbstractHibernateDAO<CrisMetrics> implements CrisMetricsDAO {
    protected CrisMetricsDAOImpl() {
        super();
    }

    @Override
    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        criteriaQuery.select(crisMetricsRoot);

        List<jakarta.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(crisMetricsRoot.get(CrisMetrics_.id)));
        criteriaQuery.orderBy(orderList);

        return list(context, criteriaQuery, false, CrisMetrics.class, limit, offset);
    }

    @Override
    public List<CrisMetrics> findAllByDSO(Context context, DSpaceObject dSpaceObject) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, DSpaceObject> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true),
                        criteriaBuilder.equal(join.get(Item_.id), dSpaceObject.getID())));
        return list(context, criteriaQuery, false, CrisMetrics.class, -1, -1);
    }

    public List<CrisMetrics> findAllLast(Context context, Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        criteriaQuery.where(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true));
        return list(context, criteriaQuery, false, CrisMetrics.class, limit, offset);
    }

    public int countAllLast(Context context) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*)"
                        + " FROM " + CrisMetrics.class.getSimpleName()
                        + " WHERE last = true");
        return count(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from CrisMetrics"));
    }

    @Override
    public void deleteByDSO(Context context, DSpaceObject dSpaceObject) throws SQLException {
        String hqlQuery = "delete from " + CrisMetrics.class.getSimpleName() + " where resource=:resource";
        Query query = createQuery(context, hqlQuery);
        query.setParameter("resource", dSpaceObject);
        query.executeUpdate();
    }

    @Override
    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceUuid)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, DSpaceObject> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.metricType), metricType),
                        criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true),
                        criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid)));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<CrisMetrics> findLastMetricsByResourceId(Context context, UUID resourceId,
            Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, DSpaceObject> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true),
                        criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceId)));
        return list(context, criteriaQuery, false, CrisMetrics.class, limit, offset);
    }

    @Override
    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
                               UUID resource, boolean last) throws SQLException {
        return null;
    }

    /* Note *
     * FIXME:
     *
     * select * from cris_metrics order by now() - timestampcreated asc limit 1
     * maybe the data where not collected in this exact day, we could consider to use the data related to the closer day
     * with a query like that select * from cris_metrics where resource_id = ? and metrictype = ?
     * and ($TARGETDATE - acquisitiondate <= $TOLLERANCE) order by $TARGETDATE - acquisitiondate asc limit 1
     */
    @Override
    public List<CrisMetrics> findMetricByResourceIdMetricTypeAndBetweenSomeDate(Context context, String metricType,
                                        UUID resourceUuid, Date before, Date after) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, DSpaceObject> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.metricType), metricType),
                criteriaBuilder.greaterThanOrEqualTo(crisMetricsRoot.get(CrisMetrics_.acquisitionDate), before),
                criteriaBuilder.lessThan(crisMetricsRoot.get(CrisMetrics_.acquisitionDate), after),
                criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid)));
        return list(context, criteriaQuery, false, CrisMetrics.class, -1, -1);
    }

}