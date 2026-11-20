/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics.embeddable.impl;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class DefaultViewEmbeddableMetricProvider extends AbstractEmbeddableMetricProvider {

    private final String TEMPLATE =
            "<a "
                    + "title=\"\" "
                    + "href=\"{{searchText}}\""
                    + ">"
                    + "View"
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
        return "embedded-view";
    }

    @Override
    public String innerHtml(Context context, Item item) {
        try {
            String prefix = StringUtils.EMPTY;
            if (!isUsageAdmin() || (isUsageAdmin() && authorizeService.isAdmin(context))) {
                prefix = configurationService.getProperty("dspace.ui.url") + "/statistics/items/" + item.getID();
            }
            return this.TEMPLATE.replace("{{searchText}}", prefix);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    @Override
    public boolean fallbackOf(final String metricType) {
        return "view".equals(metricType);
    }
}
