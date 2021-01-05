/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.dao;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.CrisMetrics_;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
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

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(crisMetricsRoot.get(CrisMetrics_.id)));
        criteriaQuery.orderBy(orderList);

        return list(context, criteriaQuery, false, CrisMetrics.class, limit, offset);
    }

    @Override
    public List<CrisMetrics> findAllByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, Item> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true),
                                    criteriaBuilder.equal(join.get(Item_.id), item.getID())));
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
    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceUuid)
           throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CrisMetrics.class);
        Root<CrisMetrics> crisMetricsRoot = criteriaQuery.from(CrisMetrics.class);
        Join<CrisMetrics, Item> join = crisMetricsRoot.join(CrisMetrics_.resource);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.metricType), metricType),
                                    criteriaBuilder.equal(crisMetricsRoot.get(CrisMetrics_.last), true),
                                    criteriaBuilder.equal(join.get(Item_.id), resourceUuid)));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
           UUID resource, boolean last) throws SQLException {
        return null;
    }

}