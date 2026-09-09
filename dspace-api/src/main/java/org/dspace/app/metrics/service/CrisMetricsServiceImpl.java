/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.service;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.dao.CrisMetricsDAO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the CrisMetrics object.
 * This class is responsible for all business logic calls for the CrisMetrics object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricsServiceImpl implements CrisMetricsService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CrisMetricsServiceImpl.class);

    @Autowired(required = true)
    protected CrisMetricsDAO crisMetricsDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public List<CrisMetrics> findAll(Context context) throws SQLException {
        return findAll(context, -1, -1);
    }

    @Override
    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return crisMetricsDAO.findAll(context, limit, offset);
    }

    @Override
    public List<CrisMetrics> findAllByDSO(Context context, DSpaceObject dSpaceObject) throws SQLException {
        return crisMetricsDAO.findAllByDSO(context, dSpaceObject);
    }

    @Override
    public List<CrisMetrics> findAllLast(Context context, Integer limit, Integer offset) throws SQLException {
        return crisMetricsDAO.findAllLast(context, limit, offset);
    }

    @Override
    public int countAllLast(Context context) throws SQLException {
        return crisMetricsDAO.countAllLast(context);
    }

    @Override
    public int count(Context context) throws SQLException {
        return crisMetricsDAO.countRows(context);
    }

    public CrisMetrics create(Context context, DSpaceObject dSpaceObject) throws SQLException, AuthorizeException {
        CrisMetrics cm = new CrisMetrics();
        cm.setResource(dSpaceObject);
        cm.setAcquisitionDate(new Date());
        CrisMetrics metric = crisMetricsDAO.create(context, cm);
        log.info(LogHelper.getHeader(context, "create_cris_metrics", "cris_metrics_id=" + metric.getId()));
        return metric;
    }

    public void delete(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException {
        this.crisMetricsDAO.delete(context, crisMetrics);
    }

    public void deleteByResourceID(Context context, DSpaceObject dSpaceObject) throws SQLException, AuthorizeException {
        this.crisMetricsDAO.deleteByDSO(context, dSpaceObject);
    }

    @Override
    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceId)
            throws SQLException {
        return this.crisMetricsDAO.findLastMetricByResourceIdAndMetricsTypes(context, metricType, resourceId);
    }

    @Override
    public List<CrisMetrics> findLastMetricsByResourceId(Context context, UUID resourceId,
            Integer limit, Integer offset) throws SQLException {
        return this.crisMetricsDAO.findLastMetricsByResourceId(context, resourceId, limit, offset);
    }

    @Override
    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(
            Context context, String metricType, UUID resource, boolean last) throws SQLException {
        return crisMetricsDAO.uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(
                context, metricType, resource, last);
    }

    @Override
    public void update(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to update a CrisMetrics");
        }
        if (!Objects.isNull(crisMetrics)) {
            crisMetricsDAO.save(context, crisMetrics);
        }
    }

    public CrisMetrics find(Context context, int id) throws SQLException {
        return crisMetricsDAO.findByID(context, CrisMetrics.class, id);
    }

    @Override
    public Optional<CrisMetrics> getCrisMetricByPeriod(Context context, String metricType, UUID resourceId,
                                                       Date startDate, String period) throws SQLException {

        if (StringUtils.equals("week", period)) {
            return getPeriodStatus(context, metricType, resourceId, DateUtils.addDays(startDate, -7));
        }
        if (StringUtils.equals("month", period)) {
            return getPeriodStatus(context, metricType, resourceId, DateUtils.addMonths(startDate, -1));
        }
        return Optional.empty();
    }

    private Optional<CrisMetrics> getPeriodStatus(Context context, String metricType, UUID resourceId, Date date)
            throws SQLException {
        List<CrisMetrics> metrics = crisMetricsDAO.findMetricByResourceIdMetricTypeAndBetweenSomeDate(context,
                metricType, resourceId, getDateByDelta(date, 0), getDateByDelta(date, +1));
        return metrics.stream().max(Comparator.comparing(CrisMetrics::getAcquisitionDate));
    }

    private Date getDateByDelta(Date date, int delta) {
        return DateUtils.setSeconds(DateUtils.setMinutes(DateUtils.setHours(DateUtils.addDays(date, delta), 0), 0), 0);
    }
}