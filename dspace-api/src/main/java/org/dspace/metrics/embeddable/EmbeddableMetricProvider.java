/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;

public interface EmbeddableMetricProvider {

    /**
     * returns weter or not a given item has a metric of the kind handled by implementing class
     * @param context
     * @param item
     * @param retrivedStoredMetrics the already retrieved stored metrics
     * @return
     */
    boolean hasMetric(Context context, Item item,  List<CrisMetrics> retrivedStoredMetrics);

    /**
     * returns weter or not implementing class support metric of provided id
     * @param metricId
     * @return
     */
    boolean support(String metricId);

    /**
     * Html snippet representing metric rendering.
     * @param context
     * @param item
     * @return
     */
    String innerHtml(Context context, Item item);

    /**
     * returns an EmbeddableCrisMetrics instance given the item id
     * @param context
     * @param item
     * @param retrivedStoredMetrics the already retrieved stored metrics
     * @return
     */
    Optional<EmbeddableCrisMetrics> provide(Context context, Item item, List<CrisMetrics> retrivedStoredMetrics);

    /**
     * returns an EmbeddableCrisMetrics instance given the metric id
     * @param context
     * @param metricId
     * @return
     * @throws SQLException
     */
    Optional<EmbeddableCrisMetrics> provide(Context context, String metricId) throws SQLException;

    /**
     *
     * @return type of metric provided by implementing class
     */
    String getMetricType();

    /**
     * Unique identifier of metric, composed with references to item and metric type.
     * @param context
     * @param item
     * @return
     */
    String getId(Context context, Item item);

    /**
     * Returns if metric supplied by this provider is a fallback of
     * type passed in input
     * @param metricType
     * @return
     */
    boolean fallbackOf(String metricType);
}
