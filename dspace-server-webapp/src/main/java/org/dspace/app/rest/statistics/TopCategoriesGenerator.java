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
import java.util.Arrays;
import java.util.HashMap;
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
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides the TopCategories related to the site.
 * <p>
 * The entity types that make up the categories are discovered from the content of the search core
 * (facet on {@code entityType_keyword}) instead of the {@code cris.entity-type} configuration property, so the
 * report self-configures to the entity types that actually have items in the repository.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class TopCategoriesGenerator extends AbstractUsageReportGenerator {

    private static final Logger log = LogManager.getLogger(TopCategoriesGenerator.class);

    private static final String OTHER_CATEGORY = "OTHER";

    private static final String ENTITY_TYPE_FIELD = "entityType_keyword";

    private static final List<String> JOIN_DEFAULT_FILTER_QUERIES = List.of("search.resourcetype:Item");

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private SolrSearchCore solrSearchCore;

    private Map<String, String> categoryQueries;

    /**
     * Creates a TopCategories usage report counting, for each configured category, the views of the items joined to
     * the given DSO within the optional date range. Each report point carries the category id and its view count.
     *
     * @param context   DSpace context
     * @param dso       the DSpace object the report is computed for
     * @param startDate optional start date (format yyyy-MM-dd) bounding the report period, may be null
     * @param endDate   optional end date (format yyyy-MM-dd) bounding the report period, may be null
     * @return Rest object containing the TopCategories usage report
     */
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
        Set<String> categoryQueriesWithItems = getCategoryQueriesWithItems(dso);

        for (String category : getCategoryQueries().keySet()) {
            String categoryQuery = getCategoryQueries().get(category);
            // a category without items in the search core has no views: its join on the statistics core is skipped
            boolean hasItems = OTHER_CATEGORY.equals(categoryQuery) || categoryQueriesWithItems == null
                || categoryQueriesWithItems.contains(categoryQuery);
            Integer categoryCount = hasItems
                ? getCategoryCount(dso, categoryQuery, startDate, endDate) : 0;
            categoriesCount.put(category, categoryCount);
        }

        return categoriesCount;

    }

    /**
     * Find, with a single query on the search core, the category queries that match at least one of the items
     * joined by the report. Returns null if the search core can not be queried: all the categories are then computed.
     */
    private Set<String> getCategoryQueriesWithItems(DSpaceObject dso) {

        List<String> categoryQueries = getCategoryQueries().values().stream()
            .filter(categoryQuery -> !OTHER_CATEGORY.equals(categoryQuery))
            .distinct()
            .collect(Collectors.toList());

        SolrQuery solrQuery = new SolrQuery(MATCH_ALL_QUERY);
        solrQuery.addFilterQuery(new StatisticsDatasetDisplay()
            .composeJoinedItemsQuery(dso, JOIN_DEFAULT_FILTER_QUERIES));
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

    private int getCategoryCount(DSpaceObject dso, String categoryQuery, String startDate, String endDate) {

        String joinFilterQuery = composeCategoryQuery(dso, categoryQuery);
        String filterQuery = new StatisticsDatasetDisplay().composeFilterQuery(startDate, endDate, true, ITEM);

        try {
            return (int) solrLoggerService.queryTotal(MATCH_ALL_QUERY, List.of(joinFilterQuery, filterQuery), 0)
                .getCount();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String composeCategoryQuery(DSpaceObject dso, String categoryQuery) {

        String query = new StatisticsDatasetDisplay().composeJoinFilterQuery(dso,
            JOIN_DEFAULT_FILTER_QUERIES, dso.getType());

        if (categoryQuery.equals(OTHER_CATEGORY)) {
            return query + " AND " + getAllCategoryQueriesReverted();
        }

        return query + " AND " + formatCategoryQuery(categoryQuery);

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

    /**
     * Returns the configured category queries keyed by category name. When no queries have been explicitly set, the
     * defaults derived from the entity types present in the repository are returned.
     *
     * @return the category queries to use for the report
     */
    public Map<String, String> getCategoryQueries() {
        if (categoryQueries == null) {
            return getDefaultCategoryQueries();
        }
        return categoryQueries;
    }

    /**
     * Sets the category queries keyed by category name, overriding the defaults discovered from the repository.
     *
     * @param categoryQueries the category queries to use
     */
    public void setCategoryQueries(Map<String, String> categoryQueries) {
        this.categoryQueries = categoryQueries;
    }

    private Map<String, String> getDefaultCategoryQueries() {
        return Arrays.stream(getDefaultEntityTypes())
                     .collect(Collectors.toMap(
                         type -> type.toLowerCase(),
                         type -> ENTITY_TYPE_FIELD + ": '" + type + "'",
                         (existing, replacement) -> existing,
                         java.util.LinkedHashMap::new
                     ));
    }

    /**
     * The entity types are discovered from the content of the
     * repository by faceting the {@code entityType_keyword} field on the search core, restricted to archived,
     * non-withdrawn items, instead of reading the {@code cris.entity-type} configuration property. Only types with
     * at least one matching item are returned, so the report categories always reflect the real content.
     *
     * @return the entity types present in the repository, possibly empty
     */
    private String[] getDefaultEntityTypes() {
        SolrQuery solrQuery = new SolrQuery(MATCH_ALL_QUERY);
        solrQuery.addFilterQuery("search.resourcetype:Item", "archived:true", "-withdrawn:true");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(ENTITY_TYPE_FIELD);
        solrQuery.setFacetMinCount(1);

        try {
            FacetField facetField = solrSearchCore.getSolr().query(solrQuery).getFacetField(ENTITY_TYPE_FIELD);
            if (facetField == null || facetField.getValues() == null) {
                return new String[0];
            }
            return facetField.getValues().stream()
                .map(FacetField.Count::getName)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        } catch (SolrServerException | IOException | RuntimeException e) {
            log.warn("Unable to discover the entity types from the search core, no category is computed", e);
            return new String[0];
        }
    }

}
