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
import java.util.Date;
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
        Context context = obtainContext();
        if (Objects.isNull(itemUuid)) {
            throw new BadRequestException();
        }
        List<CrisMetrics> metrics = findMetricsByItemUUID(context, itemUuid);
        if (metrics == null) {
            throw new ResourceNotFoundException("No such metrics found!");
        }
        return converter.toRestPage(metrics, optionalPageable, projection);
    }

    private List<CrisMetrics> findMetricsByItemUUID(Context context, UUID itemUuid) {
        indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        QueryResponse queryResponse = indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        List<SolrDocument> solrDocuments = queryResponse.getResults();
        if (solrDocuments.size() == 0) {
            return null;
        }
        SolrDocument solrDocument = solrDocuments.get(0);
        Collection<String> fields = solrDocument.getFieldNames();
        return buildCrisMetric(context, getMetricFields(fields), solrDocument);
    }

    private List<CrisMetrics> buildCrisMetric(Context context, ArrayList<String> metricFields, SolrDocument document)  {
        List<CrisMetrics> metrics = new ArrayList<CrisMetrics>(metricFields.size());
        for (String field : metricFields) {
            String[] splitedField = field.split("\\.");
            String metricType = splitedField[2];
            CrisMetrics metric = fillMetricsObject(context, document, field, metricType);
            metrics.add(metric);
        }
        return metrics;
    }

    private CrisMetrics fillMetricsObject(Context context, SolrDocument document, String field, String metricType) {
        CrisMetrics metricToFill = new CrisMetrics();
        int metricId = (int) document.getFieldValue("metric.id.".concat(metricType));
        Float metricCount = (Float) document.getFieldValue("metric.".concat(metricType));
        Date acquisitionDate = (Date) document.getFieldValue("metric.acquisitionDate.".concat(metricType));
        String remark = (String) document.getFieldValue("metric.remark.".concat(metricType));
        Double deltaPeriod1 = (Double) document.getFieldValue("metric.deltaPeriod1.".concat(metricType));
        Double deltaPeriod2 = (Double) document.getFieldValue("metric.deltaPeriod2.".concat(metricType));
        Double rank = (Double) document.getFieldValue("metric.rank.".concat(metricType));

        metricToFill.setId(metricId);
        metricToFill.setMetricType(metricType);
        metricToFill.setMetricCount(metricCount);
        metricToFill.setLast(true);
        metricToFill.setRemark(remark);
        metricToFill.setDeltaPeriod1(deltaPeriod1);
        metricToFill.setDeltaPeriod2(deltaPeriod2);
        metricToFill.setRank(rank);
        metricToFill.setAcquisitionDate(acquisitionDate);
        //TODO avoid to set the item as it is not currently used by the REST
        // and we should retrieve the real object from the session
        // (or introduce a session.load to get a lazy object by ID)
        // metricToFill.setResource(resource);
        return metricToFill;
    }

    private ArrayList<String> getMetricFields(Collection<String> fields) {
        ArrayList<String> metricsField = new ArrayList<String>();
        for (String field : fields) {
            if (field.startsWith("metric.id.")) {
                metricsField.add(field);
            }
        }
        return metricsField;
    }

}