/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "CrisMetrics" of an individual item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.METRICS)
public class ItemMetricsLinkRepository extends AbstractDSpaceRestRepository
                                       implements LinkRestRepository {

    @Autowired
    private IndexingService indexingService;

    @PreAuthorize("hasPermission(#itemUuid, 'ITEM', 'READ')")
    public Page<CrisMetrics> getMetrics(@Nullable HttpServletRequest request, @NotNull UUID itemUuid,
            @Nullable Pageable optionalPageable, Projection projection) {
        List<CrisMetrics> metrics = null;
        Context context = obtainContext();
        if (Objects.isNull(itemUuid)) {
            throw new BadRequestException();
        }
        metrics = metricsByItem(context, itemUuid);
        if (metrics == null) {
            throw new ResourceNotFoundException("No such metrics found!");
        }
        return converter.toRestPage(metrics, optionalPageable, projection);
    }

    private List<CrisMetrics> metricsByItem(Context context, UUID itemUuid) {
        indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        QueryResponse queryResponse = indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        List<SolrDocument> sd = queryResponse.getResults();
        SolrDocument document = sd.get(0);
        Collection<String> keys = document.getFieldNames();
        ArrayList<String> keysMetrics = getIdMetrics(keys);
        return buildCrisMetric(context, keysMetrics, document);
    }

    private List<CrisMetrics> buildCrisMetric(Context context, ArrayList<String> keysMetrics, SolrDocument document)  {
        List<CrisMetrics> metrics = new ArrayList<CrisMetrics>(keysMetrics.size());
        for (String key : keysMetrics) {
            String[] x = key.split("\\.");
            String type = x[2];
            CrisMetrics metric = fillMetricsObject(context, document, key, type);
            metrics.add(metric);
        }
        return metrics;
    }

    private CrisMetrics fillMetricsObject(Context context, SolrDocument document, String key, String type) {
        CrisMetrics metricToFill = new CrisMetrics();
        int metricId = (int) document.getFieldValue("metric.id.".concat(type));
        Float metricCount = (Float) document.getFieldValue("metric.".concat(type));
        String acquisitionDate = (String) document.getFieldValue("metric.acquisitionDate.".concat(type));
        metricToFill.setId(metricId);
        metricToFill.setMetricType(type);
        metricToFill.setMetricCount(metricCount);
        metricToFill.setLast(true);
        return metricToFill;
    }

    private ArrayList<String> getIdMetrics(Collection<String> keys) {
        ArrayList<String> keysMetrics = new ArrayList<String>();
        for (String key :keys) {
            if (key.startsWith("metric.id.")) {
                keysMetrics.add(key);
            }
        }
        return keysMetrics;
    }
}