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

public interface CrisItemMetricsService {

    List<CrisMetrics> getMetrics(Context context, UUID itemUuid);

    List<CrisMetrics> getStoredMetrics(Context context, UUID itemUuid);

    List<EmbeddableCrisMetrics> getEmbeddableMetrics(Context context, UUID itemUuid);

    Optional<EmbeddableCrisMetrics> getEmbeddableById(Context context, String id) throws SQLException;

}
