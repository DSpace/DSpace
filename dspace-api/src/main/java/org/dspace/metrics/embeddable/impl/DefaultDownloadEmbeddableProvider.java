/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultDownloadEmbeddableProvider extends AbstractEmbeddableMetricProvider {

    @Autowired
    private ConfigurationService configurationService;

    private final String TEMPLATE =
            "<a "
                    + "title=\"\" "
                    + "href=\"{{searchText}}\""
                    + ">"
                    + "Downloads"
                    + "</a>";


    @Override
    public boolean hasMetric(Context context, Item item, List<CrisMetrics> retrivedStoredMetrics) {
        if (retrivedStoredMetrics == null) {
            return true;
        }
        return !retrivedStoredMetrics.stream().anyMatch(m -> fallbackOf(m.getMetricType()));
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
