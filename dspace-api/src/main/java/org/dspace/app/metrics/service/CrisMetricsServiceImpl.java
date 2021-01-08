/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.service;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.dao.CrisMetricsDAO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
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
        return  findAll(context, -1, -1);
    }

    @Override
    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return  crisMetricsDAO.findAll(context, limit, offset);
    }

    @Override
    public List<CrisMetrics> findAllByItem(Context context, Item item) throws SQLException {
        return crisMetricsDAO.findAllByItem(context, item);
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

    public CrisMetrics create(Context context, Item item) throws SQLException, AuthorizeException {
        CrisMetrics cm =  new CrisMetrics();
        cm.setResource(item);
        cm.setAcquisitionDate(new Date());
        CrisMetrics metric = crisMetricsDAO.create(context, cm);
        log.info(LogManager.getHeader(context, "create_cris_metrics", "cris_metrics_id=" + metric.getId()));
        return metric;
    }

    public void delete(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException {
        this.crisMetricsDAO.delete(context, crisMetrics);
    }

    @Override
    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceId)
            throws SQLException {
        return this.crisMetricsDAO.findLastMetricByResourceIdAndMetricsTypes(context, metricType, resourceId);
    }

    @Override
    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
            UUID resource, boolean last) throws SQLException {
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
}