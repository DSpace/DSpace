/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.metrics.embeddable.EmbeddableMetricProvider;
import org.dspace.metrics.embeddable.impl.AbstractEmbeddableMetricProvider;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisItemMetricsServiceImpl implements CrisItemMetricsService {

    protected static final Logger log = LoggerFactory.getLogger(CrisItemMetricsServiceImpl.class);

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired
    protected IndexingService indexingService;

    @Autowired
    protected CrisMetricsService crisMetricsService;

    protected List<EmbeddableMetricProvider> providers;

    @Autowired
    public void setProviders(List<EmbeddableMetricProvider> providers) {
        this.providers = providers;
    }

    @Override
    public List<CrisMetrics> getMetrics(Context context, UUID itemUuid) {
        List<CrisMetrics> metrics = getStoredMetrics(context, itemUuid);
        metrics.addAll(getEmbeddableMetrics(context, itemUuid));
        return metrics;
    }

    @Override
    public List<CrisMetrics> getStoredMetrics(Context context, UUID itemUuid) {
        return findMetricsByItemUUID(context, itemUuid);
    }

    @Override
    public List<EmbeddableCrisMetrics> getEmbeddableMetrics(Context context, UUID itemUuid) {
        try {
            Item item = itemService.find(context, itemUuid);
            List<EmbeddableCrisMetrics> metrics = new ArrayList<>();
            this.providers.stream().forEach(provider -> {
                final Optional<EmbeddableCrisMetrics> metric = provider.provide(context, item);
                if (metric.isPresent()) {
                    metrics.add(metric.get());
                }
            });
            return metrics;
        } catch (SQLException ex) {
            log.warn("Item with uuid " + itemUuid + "not found");
        }
        return new ArrayList<>();
    }

    @Override
    public Optional<EmbeddableCrisMetrics> getEmbeddableById(Context context, String metricId) throws SQLException {
        for (EmbeddableMetricProvider provider : this.providers) {
            if (provider.support(metricId)) {
                return provider.provide(context, metricId);
            }
        }
        return Optional.empty();
    }

    @Override
    public CrisMetrics find(Context context, String metricId) throws SQLException {
        if (this.isEmbeddableMetricId(metricId)) {
            Optional<EmbeddableCrisMetrics> metrics = getEmbeddableById(context, metricId);
            return metrics.isPresent() ? (CrisMetrics)metrics.get() : null;
        }
        return crisMetricsService.find(context, Integer.parseInt(metricId));
    }

    private SolrDocument findMetricsDocumentInSolr(Context context, UUID itemUuid) {
        indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        QueryResponse queryResponse = indexingService.retriveSolrDocByUniqueID(itemUuid.toString());
        List<SolrDocument> solrDocuments = queryResponse.getResults();
        if (solrDocuments.size() == 0) {
            return null;
        }
        SolrDocument solrDocument = solrDocuments.get(0);
        return solrDocument;
    }

    protected List<CrisMetrics> findMetricsByItemUUID(Context context, UUID itemUuid) {
        // Solr metrics
        SolrDocument solrDocument = findMetricsDocumentInSolr(context, itemUuid);
        Collection<String> fields = Optional.ofNullable(solrDocument)
            .map(SolrDocument::getFieldNames).orElseGet(Collections::emptyList);
        List<CrisMetrics> metrics = buildCrisMetric(context, getMetricFields(fields), solrDocument);
        return metrics;
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
        Double metricCount = (Double) document.getFieldValue("metric.".concat(metricType));
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

    protected boolean isEmbeddableMetricId(String id) {
        return id.split(AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR).length == 2;
    }


}
