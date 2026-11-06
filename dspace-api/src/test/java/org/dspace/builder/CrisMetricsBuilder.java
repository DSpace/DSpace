/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.CrisItemMetricsService;

/**
 * Builder to construct CrisMetrics objects
 *
 * @author Mykhaylo Boychuk (4science)
 */
public class CrisMetricsBuilder extends AbstractBuilder<CrisMetrics, CrisMetricsService> {

    private static Logger log = LogManager.getLogger(CrisMetricsBuilder.class);

    private CrisMetrics crisMetrics;

    protected CrisMetricsBuilder(Context context) {
        super(context);
    }

    public static CrisMetricsBuilder createCrisMetrics(Context context, DSpaceObject dSpaceObject) {
        CrisMetricsBuilder builder = new CrisMetricsBuilder(context);
        return builder.create(context, dSpaceObject);
    }

    private CrisMetricsBuilder create(Context context, DSpaceObject dSpaceObject) {
        try {
            this.context = context;
            this.crisMetrics = getService().create(context, dSpaceObject);
        } catch (Exception e) {
            log.error("Error in CrisMetricsBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    protected CrisMetricsService getService() {
        return crisMetricsService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(crisMetrics);
    }

    @Override
    public CrisMetrics build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, crisMetrics);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in CrisMetricsBuilder.build(), error: ", e);
        }
        return crisMetrics;
    }

    @Override
    public void delete(Context c, CrisMetrics crisMetrics) throws Exception {
        if (crisMetrics != null) {
            getService().delete(c, crisMetrics);
        }
    }


    public void delete(CrisMetrics crisMetrics) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisMetrics attachedTab = c.reloadEntity(crisMetrics);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }
        indexingService.commit();
    }

    public static void deleteCrisMetrics(Item item) throws SQLException, IOException {
        if (item == null) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisMetrics metrics = crisMetricsService
                    .findLastMetricByResourceIdAndMetricsTypes(c, "ScopusCitation", item.getID());
            if (metrics != null) {
                try {
                    crisMetricsService.delete(c, metrics);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }

    public CrisMetricsBuilder withMetricType(String metricType) throws SQLException {
        crisMetrics.setMetricType(metricType);
        return this;
    }

    public CrisMetricsBuilder withMetricCount(double metricCount) throws SQLException {
        crisMetrics.setMetricCount(metricCount);
        return this;
    }

    public CrisMetricsBuilder isLast(boolean last) throws SQLException {
        crisMetrics.setLast(last);
        return this;
    }

    public CrisMetricsBuilder withAcquisitionDate(Date acquisitionDate) throws SQLException {
        crisMetrics.setAcquisitionDate(acquisitionDate);
        return this;
    }

    public CrisMetricsBuilder withRemark(String remark) throws SQLException {
        crisMetrics.setRemark(remark);
        return this;
    }

    public CrisMetricsBuilder withDeltaPeriod1(Double deltaPeriod1) throws SQLException {
        crisMetrics.setDeltaPeriod1(deltaPeriod1);
        return this;
    }

    public CrisMetricsBuilder withDeltaPeriod2(Double deltaPeriod2) throws SQLException {
        crisMetrics.setDeltaPeriod2(deltaPeriod2);
        return this;
    }

    public CrisMetricsBuilder withRank(Double rank) throws SQLException {
        crisMetrics.setRank(rank);
        return this;
    }

    public static String getRestStoredMetricId(Integer metricId) {
        return CrisItemMetricsService.STORED_METRIC_ID_PREFIX + metricId;
    }

}