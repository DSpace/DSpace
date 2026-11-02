/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import static org.dspace.core.Constants.ITEM;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.dspace.app.rest.model.UsageReportPointCategoryRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides the TopCategories related to the site.
 * <p>
 * DURACOM-497: the entity types that make up the categories are discovered from the content of the search core
 * (facet on {@code entityType_keyword}) instead of the {@code cris.entity-type} configuration property, so the
 * report self-configures to the entity types that actually have items in the repository. The views of each category
 * are counted on the statistics core through a cross-core join towards the search core, restricted to archived,
 * non-withdrawn items of the given entity type.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class TopCategoriesGenerator extends AbstractUsageReportGenerator {

    private static final Logger log = LogManager.getLogger(TopCategoriesGenerator.class);

    private static final String OTHER_CATEGORY = "OTHER";

    private static final String ENTITY_TYPE_FIELD = "entityType_keyword";

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private SolrSearchCore solrSearchCore;

    @Autowired
    private ConfigurationService configurationService;

    private Map<String, String> categoryQueries;

    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate) {

        Map<String, Integer> categoriesCount = getCategoriesCount(dso, startDate, endDate);

        UsageReportRest usageReportRest = new UsageReportRest();

        for (String category : categoriesCount.keySet()) {
            UsageReportPointCategoryRest categoryPoint = new UsageReportPointCategoryRest();
            categoryPoint.setId(category);
            categoryPoint.addValue("views", categoriesCount.get(category));
            usageReportRest.addPoint(categoryPoint);
        }

        return usageReportRest;
    }

    private Map<String, Integer> getCategoriesCount(DSpaceObject dso, String startDate, String endDate) {

        Map<String, Integer> categoriesCount = new HashMap<String, Integer>();
        Set<String> categoryQueriesWithItems = getCategoryQueriesWithItems();

        for (String category : getCategoryQueries().keySet()) {
            String categoryQuery = getCategoryQueries().get(category);
            // a category without items in the search core has no views: its join on the statistics core is skipped
            boolean hasItems = OTHER_CATEGORY.equals(categoryQuery) || categoryQueriesWithItems == null
                || categoryQueriesWithItems.contains(categoryQuery);
            Integer categoryCount = hasItems ? getCategoryCount(categoryQuery, startDate, endDate) : 0;
            categoriesCount.put(category, categoryCount);
        }

        return categoriesCount;

    }

    /**
     * Find, with a single query on the search core, the category queries that match at least one archived,
     * non-withdrawn item. Returns null if the search core can not be queried: all the categories are then computed.
     */
    private Set<String> getCategoryQueriesWithItems() {

        List<String> categoryQueries = getCategoryQueries().values().stream()
            .filter(categoryQuery -> !OTHER_CATEGORY.equals(categoryQuery))
            .distinct()
            .collect(Collectors.toList());

        SolrQuery solrQuery = new SolrQuery(MATCH_ALL_QUERY);
        solrQuery.addFilterQuery("search.resourcetype:Item", "archived:true", "-withdrawn:true");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        categoryQueries.forEach(categoryQuery -> solrQuery.addFacetQuery(formatCategoryQuery(categoryQuery)));

        try {
            Map<String, Integer> facetQueryCounts = solrSearchCore.getSolr().query(solrQuery).getFacetQuery();
            return categoryQueries.stream()
                .filter(categoryQuery -> facetQueryCounts.getOrDefault(formatCategoryQuery(categoryQuery), 0) > 0)
                .collect(Collectors.toSet());
        } catch (SolrServerException | IOException | RuntimeException e) {
            log.warn("Unable to find the categories with items, all the categories are computed", e);
            return null;
        }
    }

    private int getCategoryCount(String categoryQuery, String startDate, String endDate) {

        String joinQuery = composeCategoryQuery(categoryQuery);
        String filterQuery = new StatisticsDatasetDisplay().composeFilterQuery(startDate, endDate, true, ITEM);

        try {
            // the cross-core join is the main query ({!join ...} local params must start the query, not be nested
            // inside a filter query); the statistics filters are applied as the (cacheable) filter query
            return (int) solrLoggerService.queryTotal(joinQuery, filterQuery, 0).getCount();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build the cross-core join filter query that selects, on the statistics core, the usage events of the items
     * matching the given category. The join target is the search core, restricted to archived, non-withdrawn items.
     * <p>
     * The discovery default filter queries are intentionally NOT concatenated here: one of them is a purely negative
     * clause ("-withdrawn:true AND -discoverable:false") that Solr resolves to zero matches once nested inside the
     * joined query. The {@code entityType_keyword} field only exists on indexed items, so the archived/withdrawn
     * filters are sufficient to select the valid items (DURACOM-497).
     */
    private String composeCategoryQuery(String categoryQuery) {

        String joinPrefix = "{!join from=search.resourceid to=id fromIndex="
            + configurationService.getProperty("solr.multicorePrefix") + "search}";
        String itemsFilter = " AND archived:true AND -withdrawn:true";

        if (categoryQuery.equals(OTHER_CATEGORY)) {
            return joinPrefix + getAllCategoryQueriesReverted() + itemsFilter;
        }

        return joinPrefix + formatCategoryQuery(categoryQuery) + itemsFilter;

    }

    private String getAllCategoryQueriesReverted() {
        return getCategoryQueries().values().stream()
            .filter(categoryQuery -> !OTHER_CATEGORY.equals(categoryQuery))
            .map(categoryQuery -> "-" + formatCategoryQuery(categoryQuery))
            .collect(Collectors.joining(" AND "));
    }

    private String formatCategoryQuery(String categoryQuery) {
        return "(" + categoryQuery.replaceAll("'", "\"") + ")";
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOP_CATEGORIES_REPORT_ID;
    }

    public Map<String, String> getCategoryQueries() {
        if (categoryQueries == null) {
            return getDefaultCategoryQueries();
        }
        return categoryQueries;
    }

    public void setCategoryQueries(Map<String, String> categoryQueries) {
        this.categoryQueries = categoryQueries;
    }

    private Map<String, String> getDefaultCategoryQueries() {
        return getDefaultEntityTypes().stream()
                     .collect(Collectors.toMap(
                         type -> type.toLowerCase(),
                         type -> ENTITY_TYPE_FIELD + ": '" + type + "'",
                         (existing, replacement) -> existing,
                         LinkedHashMap::new
                     ));
    }

    /**
     * Discover the entity types that actually have items, by faceting the {@code entityType_keyword} field on the
     * search core. Only types with at least one archived, non-withdrawn item are returned, so the report categories
     * always reflect the real content of the repository.
     *
     * @return the entity types present in the repository, possibly empty
     */
    private List<String> getDefaultEntityTypes() {
        SolrQuery solrQuery = new SolrQuery(MATCH_ALL_QUERY);
        solrQuery.addFilterQuery("search.resourcetype:Item", "archived:true", "-withdrawn:true");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(ENTITY_TYPE_FIELD);
        solrQuery.setFacetMinCount(1);

        try {
            FacetField facetField = solrSearchCore.getSolr().query(solrQuery).getFacetField(ENTITY_TYPE_FIELD);
            if (facetField == null || facetField.getValues() == null) {
                return List.of();
            }
            return facetField.getValues().stream()
                .map(FacetField.Count::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        } catch (SolrServerException | IOException | RuntimeException e) {
            log.warn("Unable to discover the entity types from the search core, no category is computed", e);
            return List.of();
        }
    }

}
