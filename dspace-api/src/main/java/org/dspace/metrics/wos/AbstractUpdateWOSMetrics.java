/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.MetricsExternalServices;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public abstract class AbstractUpdateWOSMetrics implements MetricsExternalServices {

    private static final Logger log = LogManager.getLogger(AbstractUpdateWOSMetrics.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected CrisMetricsService crisMetricsService;

    @Override
    public abstract boolean updateMetric(Context context, Item item, String param);

    protected boolean updateWosMetric(Context context, Item currentItem, CrisMetricDTO metricDTO) {
        try {
            if (Objects.isNull(metricDTO) || StringUtils.isBlank(metricDTO.getMetricType())) {
                return false;
            }
            CrisMetrics wosMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                        metricDTO.getMetricType(), currentItem.getID());
            if (!Objects.isNull(wosMetrics)) {
                wosMetrics.setLast(false);
                crisMetricsService.update(context, wosMetrics);
            }
            Optional<CrisMetrics> metricLastWeek = crisMetricsService.getCrisMetricByPeriod(context,
                                                   metricDTO.getMetricType(), currentItem.getID(), new Date(), "week");
            Optional<CrisMetrics> metricLastMonth = crisMetricsService.getCrisMetricByPeriod(context,
                                                   metricDTO.getMetricType(), currentItem.getID(), new Date(), "month");

            Double deltaPeriod1 = getDeltaPeriod(metricDTO, metricLastWeek);
            Double deltaPeriod2 = getDeltaPeriod(metricDTO, metricLastMonth);

            createNewWosMetric(context, currentItem, metricDTO, deltaPeriod1, deltaPeriod2);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        }
        return true;
    }

    private void createNewWosMetric(Context context, Item item, CrisMetricDTO metcitDTO,
            Double deltaPeriod1, Double deltaPeriod2) throws SQLException, AuthorizeException {
        CrisMetrics newWosMetric = crisMetricsService.create(context, item);
        newWosMetric.setMetricType(metcitDTO.getMetricType());
        newWosMetric.setLast(true);
        newWosMetric.setMetricCount(metcitDTO.getMetricCount());
        newWosMetric.setAcquisitionDate(new Date());
        newWosMetric.setDeltaPeriod1(deltaPeriod1);
        newWosMetric.setDeltaPeriod2(deltaPeriod2);
    }

    protected Double getDeltaPeriod(CrisMetricDTO currentMetric, Optional<CrisMetrics> metric) {
        if (!metric.isEmpty()) {
            return currentMetric.getMetricCount() - metric.get().getMetricCount();
        }
        return null;
    }

}