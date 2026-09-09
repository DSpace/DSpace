/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.MetricsExternalServices;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class UpdateScopusMetrics extends MetricsExternalServices {

    private static final String EMPTY_STRING = "";
    private static Logger log = LogManager.getLogger(UpdateScopusMetrics.class);

    public static final String SCOPUS_CITATION = "scopusCitation";

    private List<String> logsCache = new ArrayList<>();

    @Autowired
    private ScopusProvider scopusProvider;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    public String getServiceName() {
        return "scopus";
    }

    @Override
    public List<String> getFilters() {
        return Arrays.asList("dspace.entity.type:Publication", "dc.identifier.doi:* OR dc.identifier.pmid:*");
    }

    public List<String> getLogs() {
        return logsCache;
    }

    @Override
    public boolean updateMetric(Context context, Item item, String param) {
        String id = buildQuery(item);
        CrisMetricDTO scopusMetric = scopusProvider.getScopusObject(id);
        if (Objects.isNull(scopusMetric)) {
            return false;
        }
        return updateScopusMetrics(context, item, scopusMetric);
    }

    @Override
    public long updateMetric(Context context, Iterator<Item> itemIterator, String param) {
        long updatedItems = 0;
        long foundItems = 0;
        long apiCalls = 0;
        logsCache = new ArrayList<>();
        try {
            while (itemIterator.hasNext()) {
                Map<String, Item> queryMap = new HashMap<>();
                List<Item> itemList = new ArrayList<>();
                for (int i = 0; i < fetchSize && itemIterator.hasNext(); i++) {
                    Item item = itemIterator.next();
                    logAndCache("Adding item with uuid: " + item.getID());
                    setLastImportMetadataValue(context, item);
                    itemList.add(item);
                }
                foundItems += itemList.size();
                String id = this.generateQuery(queryMap, itemList);
                logAndCache("Getting scopus metrics for " + id);
                updatedItems +=
                        scopusProvider.getScopusList(this.generateQuery(queryMap, itemList))
                            .stream()
                            .filter(Objects::nonNull)
                            .map(scopusMetric -> this.updateScopusMetrics(
                                    context,
                                    this.findItem(queryMap, scopusMetric),
                                    scopusMetric
                                )
                            )
                            .filter(BooleanUtils::isTrue)
                            .count();
                apiCalls++;
                context.commit();
            }
        } catch (SQLException e) {
            logAndCacheError("Error while updating scopus' metrics", e);
        } finally {
            logAndCache("Found and fetched " + foundItems + " with " + apiCalls + " api calls!");
        }
        logsCache.addAll(scopusProvider.getLogs());
        return updatedItems;
    }

    private Item findItem(Map<String, Item> queryMap, CrisMetricDTO scopusMetric) {
        if (queryMap == null || scopusMetric == null) {
            return null;
        }
        return List.of(
                mapClause(scopusMetric.getTmpRemark().get("doi"), "DOI"),
                mapClause(scopusMetric.getTmpRemark().get("pmid"), "PMID"),
                mapClause(scopusMetric.getIdentifier(), "EID")
            )
            .stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(identifierClause -> queryMap.get(identifierClause.toString()))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String generateQuery(Map<String, Item> queryMap, List<Item> itemList) {
        return itemList
                .stream()
                .map(item -> new StringBuilder(this.buildQuery(queryMap, item)))
                .filter(StringUtils::isNotEmpty)
                .reduce(joiningOr())
                .map(StringBuilder::toString)
                .orElse(null);
    }

    private String buildQuery(Map<String, Item> queryMap, Item item) {
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        String pmid = itemService.getMetadataFirstValue(item, "dc", "identifier", "pmid", Item.ANY);
        String scopus = itemService.getMetadataFirstValue(item, "dc", "identifier", "scopus", Item.ANY);
        return List.of(
                    mapClause(doi, "DOI", queryMap, item),
                    mapClause(pmid, "PMID", queryMap, item),
                    mapClause(scopus, "EID", queryMap, item)
                )
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(joiningOr())
                .map(StringBuilder::toString)
                .orElse(null);
    }

    private String buildQuery(Item item) {
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        String pmid = itemService.getMetadataFirstValue(item, "dc", "identifier", "pmid", Item.ANY);
        String scopus = itemService.getMetadataFirstValue(item, "dc", "identifier", "scopus", Item.ANY);
        return List.of(
            mapClause(doi, "DOI"),
            mapClause(pmid, "PMID"),
            mapClause(scopus, "EID")
        )
            .stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(joiningOr())
            .map(StringBuilder::toString)
            .orElse(null);
    }

    private BinaryOperator<StringBuilder> joiningOr() {
        return (query, clause) -> query.append(" OR ").append(clause);
    }

    private Optional<StringBuilder> mapClause(String field, String function) {
        StringBuilder clause = null;
        if (StringUtils.isNotEmpty(field)) {
            clause = new StringBuilder(function).append("(").append(field).append(")");
        }
        return Optional.ofNullable(clause);
    }

    private Optional<StringBuilder> mapClause(String field, String function, Map<String, Item> queryMap, Item item) {
        Optional<StringBuilder> generatedClause = this.mapClause(field, function);
        if (queryMap != null && generatedClause.isPresent()) {
            queryMap.putIfAbsent(generatedClause.get().toString(), item);
        }
        return generatedClause;
    }

    private boolean updateScopusMetrics(Context context, Item currentItem, CrisMetricDTO scopusMetric) {
        try {
            if (scopusMetric == null || currentItem == null) {
                return false;
            }
            CrisMetrics scopusMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                                        SCOPUS_CITATION, currentItem.getID());
            if (!Objects.isNull(scopusMetrics)) {
                scopusMetrics.setLast(false);
                crisMetricsService.update(context, scopusMetrics);
            }
            Optional<CrisMetrics> metricLastWeek = crisMetricsService.getCrisMetricByPeriod(context,
                                                   SCOPUS_CITATION, currentItem.getID(), new Date(), "week");
            Optional<CrisMetrics> metricLastMonth = crisMetricsService.getCrisMetricByPeriod(context,
                                                    SCOPUS_CITATION, currentItem.getID(), new Date(), "month");

            Double deltaPeriod1 = getDeltaPeriod(scopusMetric, metricLastWeek);
            Double deltaPeriod2 = getDeltaPeriod(scopusMetric, metricLastMonth);

            createNewScopusMetrics(context,currentItem, scopusMetric, deltaPeriod1, deltaPeriod2);
        } catch (SQLException | AuthorizeException e) {
            logsCache.add(e.getMessage());
            log.error(e.getMessage(), e);
        }
        return true;
    }

    private void createNewScopusMetrics(Context context, Item item, CrisMetricDTO scopusMetric,
            Double deltaPeriod1, Double deltaPeriod2) throws SQLException, AuthorizeException {
        CrisMetrics newScopusMetrics = crisMetricsService.create(context, item);
        newScopusMetrics.setMetricType(SCOPUS_CITATION);
        newScopusMetrics.setLast(true);
        newScopusMetrics.setMetricCount(scopusMetric.getMetricCount());
        newScopusMetrics.setAcquisitionDate(new Date());
        newScopusMetrics.setRemark(parseScopusMetricRemark(scopusMetric));
        newScopusMetrics.setDeltaPeriod1(deltaPeriod1);
        newScopusMetrics.setDeltaPeriod2(deltaPeriod2);
    }

    private static String parseScopusMetricRemark(CrisMetricDTO scopusMetric) {
        return Optional.ofNullable(scopusMetric.getRemark())
                       .filter(Objects::nonNull)
                       .map(s -> s.replaceAll("link", "detailUrl"))
                       .orElseGet(() -> EMPTY_STRING);
    }

    private Double getDeltaPeriod(CrisMetricDTO currentMetric, Optional<CrisMetrics> metric) {
        if (!metric.isEmpty()) {
            return currentMetric.getMetricCount() - metric.map(CrisMetrics::getMetricCount).orElse(Double.valueOf(0));
        }
        return null;
    }

    private void logAndCache(String message) {
        logsCache.add("INFO: " + message);
        log.info(message);
    }

    private void logAndCacheError(String message, Throwable e) {
        logsCache.add("ERROR: " + message + '\n' + e.getMessage());
        log.error(message, e);
    }
}
