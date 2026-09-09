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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import org.dspace.metricsSecurity.BoxMetricsLayoutConfigurationService;
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

    @Autowired
    protected BoxMetricsLayoutConfigurationService boxMetricsLayoutConfigurationService;

    @Override
    public List<CrisMetrics> getMetrics(Context context, UUID itemUuid) {
        // searches in solr
        List<CrisMetrics> metrics = getStoredMetrics(context, itemUuid);
        metrics.addAll(getEmbeddableMetrics(context, itemUuid, metrics));
        return metrics;
    }

    @Override
    public List<CrisMetrics> getStoredMetrics(Context context, UUID itemUuid) {
        return findMetricsByItemUUID(context, itemUuid);
    }

    @Override
    public List<EmbeddableCrisMetrics> getEmbeddableMetrics(Context context, UUID itemUuid,
            List<CrisMetrics> retrivedStoredMetrics) {
        try {
            Item item = itemService.find(context, itemUuid);
            return this.providers.stream()
                .flatMap(provider -> provider.provide(context, item, retrivedStoredMetrics).stream())
                .filter(metric -> checkPermissionsOfMetricsByBox(context, item, metric))
                .collect(Collectors.toList());
        } catch (SQLException ex) {
            log.warn("Item with uuid " + itemUuid + "not found");
        }
        return new ArrayList<>();
    }

    @Override
    public Optional<EmbeddableCrisMetrics> getEmbeddableById(Context context, String metricId) throws SQLException {
        for (EmbeddableMetricProvider provider : this.providers) {
            if (provider.support(metricId)) {
                Optional<EmbeddableCrisMetrics> embeddableCrisMetrics =  provider.provide(context, metricId);
                if (embeddableCrisMetrics.isPresent()) {
                    if (checkPermissionsOfMetricsByBox(context,
                            itemFromMetricId(context, metricId), embeddableCrisMetrics.get())) {
                        return embeddableCrisMetrics;
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> embeddableFallback(final String metricType) {
        return providers.stream()
                .filter(p -> p.fallbackOf(metricType))
                .map(p -> p.getMetricType())
                .findFirst();
    }

    @Override
    public CrisMetrics find(Context context, String metricId) throws SQLException {
        if (this.isEmbeddableMetricId(metricId)) {
            Optional<EmbeddableCrisMetrics> metrics = getEmbeddableById(context, metricId);
            if (metrics.isPresent()) {
                if (checkPermissionsOfMetricsByBox(context, itemFromMetricId(context, metricId), metrics.get())) {
                    return (CrisMetrics) metrics.get();
                }
            } else {
                return null;
            }
        }
        if (StringUtils.startsWith(metricId, STORED_METRIC_ID_PREFIX)) {
            CrisMetrics crisMetrics = crisMetricsService.find(context,
                    Integer.parseInt(metricId.substring(STORED_METRIC_ID_PREFIX.length())));
            if (checkPermissionsOfMetricsByBox(context, itemFromMetricId(context, metricId), crisMetrics)) {
                return crisMetrics;
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean isEmbeddableMetricId(String id) {
        return !StringUtils.startsWith(id, STORED_METRIC_ID_PREFIX);
    }

    private SolrDocument findMetricsDocumentInSolr(Context context, UUID itemUuid) {
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

    private List<CrisMetrics> buildCrisMetric(Context context, ArrayList<String> metricFields, SolrDocument document) {
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

    public EmbeddableCrisMetrics createEmbeddableCrisMetrics(String remark, String uuid) {
        EmbeddableCrisMetrics embeddableCrisMetrics = new EmbeddableCrisMetrics();
        embeddableCrisMetrics.setMetricType("embedded-view");
        embeddableCrisMetrics.setRemark(remark);
        embeddableCrisMetrics.setEmbeddableId(uuid + ":" + "embedded-view");
        return embeddableCrisMetrics;
    }

    public boolean checkPermissionsOfMetricsByBox(Context context, Item item, CrisMetrics crisMetric) {
        return boxMetricsLayoutConfigurationService.checkPermissionOfMetricByBox(context, item, crisMetric);
    }

    public Item itemFromMetricId(Context context, String target) throws SQLException {
        if (isEmbeddableMetricId(target.toString())) {
            int indexOf = target.indexOf(AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR);
            if (indexOf != -1) {
                String uuid = target.substring(0, indexOf);
                return itemService.find(context, UUID.fromString(uuid));
            } else {
                return null;
            }

        } else {
            CrisMetrics crisMetrics = crisMetricsService.find(context,
                    Integer.parseInt(target.substring(STORED_METRIC_ID_PREFIX.length())));
            return crisMetrics != null ? itemService.find(context, crisMetrics.getResource().getID()) : null;
        }
    }
}
