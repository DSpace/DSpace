/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.sql.SQLException;
import java.util.Objects;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultDownloadEmbeddableProvider extends AbstractEmbeddableMetricProvider {

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CrisMetricsService crisMetricsService;

    private final String TEMPLATE =
            "<a "
                    + "title=\"\" "
                    + "href=\"{{searchText}}\""
                    + ">"
                    + "Downloads"
                    + "</a>";


    @Override
    public boolean hasMetric(Context context, Item item) {
        try {
            final CrisMetrics view = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(
                context,
                "download",
                item.getID());
            return Objects.isNull(view);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getMetricType() {
        return "embedded-download";
    }

    @Override
    public String innerHtml(Context context, Item item) {
        String prefix = configurationService.getProperty("dspace.ui.url") + "/statistics/items/" + item.getID();
        return this.TEMPLATE.replace("{{searchText}}", prefix);
    }

    @Override
    public boolean fallbackOf(final String metricType) {
        return "download".equals(metricType);
    }
}
