/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.core.Context;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;

/**
 * Service to find  {@link CrisMetrics} given an Item UUID.
 * Two kind of metrics are given: embeddable and stored.
 * The first kind means a metric that is not stored on the database but is provided
 * by an external service and embedded into our response, the second kind means a metric
 * that is always provided by an external service, but this service is queried asynchronously and its
 * returned values are stored on DSpace database.
 */
public interface CrisItemMetricsService {

    /**
     * Returns both embeddable and stored metrics.
     * @param context
     * @param itemUuid
     * @return
     */
    List<CrisMetrics> getMetrics(Context context, UUID itemUuid);

    /**
     * returns only Stored metrics
     * @param context
     * @param itemUuid
     * @return
     */
    List<CrisMetrics> getStoredMetrics(Context context, UUID itemUuid);

    /**
     * Returns only embeddable metrics
     * @param context
     * @param itemUuid
     * @return
     */
    List<EmbeddableCrisMetrics> getEmbeddableMetrics(Context context, UUID itemUuid);

    /**
     * returns an {@link EmbeddableCrisMetrics} given a metric id.
     * @param context
     * @param id
     * @return
     * @throws SQLException
     */
    Optional<EmbeddableCrisMetrics> getEmbeddableById(Context context, String id) throws SQLException;

    /**
     * find a {@link CrisMetrics} given a metricId
     * @param context
     * @param metricId
     * @return
     * @throws SQLException
     */
    CrisMetrics find(Context context, String metricId) throws SQLException;

}
