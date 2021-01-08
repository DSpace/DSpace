/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable;

import java.sql.SQLException;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;

public interface EmbeddableMetricProvider {

    boolean hasMetric(Context context, Item item);

    boolean support(String metricId);

    String innerHtml(Context context, Item item);

    Optional<EmbeddableCrisMetrics> provide(Context context, Item item);

    Optional<EmbeddableCrisMetrics> provide(Context context, String metricId) throws SQLException;

    String getMetricType();

    String getId(Context context, Item item);

}
