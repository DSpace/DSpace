/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SolrServiceMetricsIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = LogManager.getLogger(SolrServiceMetricsIndexPlugin.class);

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        if (idxObj instanceof IndexableItem) {
            Item item = ((IndexableItem) idxObj).getIndexedObject();
            if (Objects.nonNull(item)) {
                try {
                    List<CrisMetrics> metrics = crisMetricsService.findAllByDSO(context, item);
                    for (CrisMetrics metric : metrics) {
                        SearchUtils.addMetricFieldsInSolrDoc(metric, document);
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

}