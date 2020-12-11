/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.wos;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.externalservices.MetricsExternalServices;
import org.dspace.externalservices.scopus.UpdateScopusMetrics;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateWOSMetrics implements MetricsExternalServices {

    private static Logger log = LogManager.getLogger(UpdateScopusMetrics.class);

    public static final String WOS_METRIC_TYPE = "wosCitation";

    @Autowired
    private WOSProvider wosProvider;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    public boolean updateMetric(Context context, Item item) {
        Double metricCount = null;
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        if (!StringUtils.isBlank(doi)) {
            metricCount = wosProvider.getWOSObject(doi);
        }
        return updateScopusMetrics(context, item, metricCount);
    }

    private boolean updateScopusMetrics(Context context, Item currentItem, Double metricCount) {
        try {
            if (Objects.isNull(metricCount)) {
                return false;
            }
            CrisMetrics scopusMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                    WOS_METRIC_TYPE, currentItem.getID());
            if (!Objects.isNull(scopusMetrics)) {
                scopusMetrics.setLast(false);
            }
            createNewScopusMetrics(context, currentItem, metricCount);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        } catch (AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        }
        return true;
    }

    private void createNewScopusMetrics(Context context, Item item, Double metricCount)
            throws SQLException, AuthorizeException {
        CrisMetrics newWosMetric = crisMetricsService.create(context, item);
        newWosMetric.setMetricType(WOS_METRIC_TYPE);
        newWosMetric.setLast(true);
        newWosMetric.setMetricCount(metricCount);
        newWosMetric.setAcquisitionDate(new Date());
    }
}