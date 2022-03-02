/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscoverQueryBuilder implements InitializingBean {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DiscoverQueryBuilder.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private List<IndexFactory> indexableFactories;

    private int pageSizeLimit;

    @Override
    public void afterPropertiesSet() throws Exception {
        pageSizeLimit = configurationService.getIntProperty("rest.search.max.results", 100);
    }

    /**
     * Build a discovery query
     *
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoType                only include search results with this type
     * @param pageSize               the page size for this discovery query
     * @param offset                 the offset for this discovery query
     * @param sortProperty           the sort property for this discovery query
     * @param sortDirection          the sort direction for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<QueryBuilderSearchFilter> searchFilters,
                                    String dsoType, Integer pageSize, Long offset, String sortProperty,
                                    String sortDirection) throws SearchServiceException {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildQuery(context, scope, discoveryConfiguration, query, searchFilters, dsoTypes, pageSize, offset,
                          sortProperty, sortDirection);
    }


    /**
     * Build a discovery query
     *
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoTypes               only include search results with one of these types
     * @param pageSize               the page size for this discovery query
     * @param offset                 the offset for this discovery query
     * @param sortProperty           the sort property for this discovery query
     * @param sortDirection          the sort direction for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<QueryBuilderSearchFilter> searchFilters,
                                    List<String> dsoTypes, Integer pageSize, Long offset, String sortProperty,
                                    String sortDirection)
            throws IllegalArgumentException, SearchServiceException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                                                           dsoTypes);

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting
        configurePagination(pageSize, offset, queryArgs);
        configureSorting(sortProperty, sortDirection, queryArgs, discoveryConfiguration.getSearchSortConfiguration());

        addDiscoveryHitHighlightFields(discoveryConfiguration, queryArgs);
        return queryArgs;
    }

    private void addDiscoveryHitHighlightFields(DiscoveryConfiguration discoveryConfiguration,
                                                DiscoverQuery queryArgs) {
        if (discoveryConfiguration.getHitHighlightingConfiguration() != null) {
            List<DiscoveryHitHighlightFieldConfiguration> metadataFields = discoveryConfiguration
                    .getHitHighlightingConfiguration().getMetadataFields();
            for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : metadataFields) {
                queryArgs.addHitHighlightingField(
                        new DiscoverHitHighlightingField(fieldConfiguration.getField(), fieldConfiguration.getMaxSize(),
                                                         fieldConfiguration.getSnippets()));
            }
        }
    }

    /**
     * Create a discovery facet query.
     *
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param prefix                 limit the facets results to those starting with the given prefix.
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoType                only include search results with this type
     * @param pageSize               the page size for this discovery query
     * @param offset                 the offset for this discovery query
     * @param facetName              the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<QueryBuilderSearchFilter> searchFilters,
                                         String dsoType, Integer pageSize, Long offset, String facetName)
            throws IllegalArgumentException {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildFacetQuery(
                context, scope, discoveryConfiguration, prefix, query, searchFilters, dsoTypes, pageSize, offset,
                facetName);
    }

    /**
     * Create a discovery facet query.
     *
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param prefix                 limit the facets results to those starting with the given prefix.
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoTypes               only include search results with one of these types
     * @param pageSize               the page size for this discovery query
     * @param offset                 the offset for this discovery query
     * @param facetName              the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<QueryBuilderSearchFilter> searchFilters,
                                         List<String> dsoTypes, Integer pageSize, Long offset, String facetName)
            throws IllegalArgumentException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                                                           dsoTypes);

        //When all search criteria are set, configure facet results
        addFacetingForFacets(context, scope, prefix, queryArgs, discoveryConfiguration, facetName, pageSize);

        //We don' want any search results, we only want facet values
        queryArgs.setMaxResults(0);

        //Configure pagination
        configurePaginationForFacets(offset, queryArgs);

        return queryArgs;
    }

    private void configurePaginationForFacets(Long offset, DiscoverQuery queryArgs) {
        if (offset != null) {
            queryArgs.setFacetOffset(Math.toIntExact(offset));
        }
    }

    private DiscoverQuery addFacetingForFacets(Context context, IndexableObject scope, String prefix,
                                               DiscoverQuery queryArgs, DiscoveryConfiguration discoveryConfiguration,
                                               String facetName, Integer pageSize)
            throws IllegalArgumentException {

        DiscoverySearchFilterFacet facet = discoveryConfiguration.getSidebarFacet(facetName);
        if (facet != null) {
            queryArgs.setFacetMinCount(1);

            pageSize = pageSize != null ? Math.min(pageSizeLimit, pageSize) : pageSizeLimit;

            fillFacetIntoQueryArgs(context, scope, prefix, queryArgs, facet, pageSize);

        } else {
            throw new IllegalArgumentException(facetName + " is not a valid search facet");
        }

        return queryArgs;
    }

    private void fillFacetIntoQueryArgs(Context context, IndexableObject scope, String prefix,
                                        DiscoverQuery queryArgs, DiscoverySearchFilterFacet facet, final int pageSize) {
        if (facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            try {
                FacetYearRange facetYearRange =
                        searchService.getFacetYearRange(context, scope, facet, queryArgs.getFilterQueries(), queryArgs);

                queryArgs.addYearRangeFacet(facet, facetYearRange);

            } catch (Exception e) {
                log.error(LogHelper.getHeader(context, "Error in Discovery while setting up date facet range",
                                               "date facet: " + facet), e);
            }

        } else {

            //Add one to our facet limit to make sure that if we have more then the shown facets that we show our
            // "show more" url
            int facetLimit = pageSize + 1;
            //This should take care of the sorting for us
            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit,
                                                           facet.getSortOrderSidebar(),
                                                           StringUtils.trimToNull(prefix)));
        }
    }

    private DiscoverQuery buildCommonDiscoverQuery(Context context, DiscoveryConfiguration discoveryConfiguration,
                                                   String query,
                                                   List<QueryBuilderSearchFilter> searchFilters, List<String> dsoTypes)
            throws IllegalArgumentException {
        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration);

        queryArgs.addFilterQueries(convertFiltersToString(context, discoveryConfiguration, searchFilters));

        //Set search query
        if (StringUtils.isNotBlank(query)) {
            queryArgs.setQuery(query);
        }

        //Limit results to DSO types
        if (isNotEmpty(dsoTypes)) {
            dsoTypes.stream()
                    .map(this::getDsoType)
                    .forEach(queryArgs::addDSpaceObjectFilter);
        }

        return queryArgs;
    }

    private DiscoverQuery buildBaseQueryForConfiguration(DiscoveryConfiguration discoveryConfiguration) {
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.setDiscoveryConfigurationName(discoveryConfiguration.getId());
        queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries()
                                                         .toArray(
                                                                 new String[discoveryConfiguration
                                                                         .getDefaultFilterQueries()
                                                                         .size()]));
        return queryArgs;
    }

    private void configureSorting(String sortProperty, String sortDirection, DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration)
            throws IllegalArgumentException, SearchServiceException {
        String sortBy = sortProperty;
        String sortOrder = sortDirection;

        //Load defaults if we did not receive values
        if (sortBy == null) {
            sortBy = getDefaultSortField(searchSortConfiguration);
        }
        if (sortOrder == null) {
            sortOrder = getDefaultSortDirection(searchSortConfiguration, sortOrder);
        }

        if (StringUtils.isNotBlank(sortBy) && !isConfigured(sortBy, searchSortConfiguration)) {
            throw new SearchServiceException(
                    "The field: " + sortBy + "is not configured for the configuration!");
        }


        //Update Discovery query
        DiscoverySortFieldConfiguration sortFieldConfiguration = searchSortConfiguration
                .getSortFieldConfiguration(sortBy);

        if (sortFieldConfiguration != null) {
            String sortField = searchService
                    .toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());

            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
            } else {
                throw new IllegalArgumentException(sortOrder + " is not a valid sort order");
            }

        } else {
            throw new IllegalArgumentException(sortBy + " is not a valid sort field");
        }
    }

    private boolean isConfigured(String sortBy, DiscoverySortConfiguration searchSortConfiguration) {
        return Objects.nonNull(searchSortConfiguration.getSortFieldConfiguration(sortBy));
    }

    private String getDefaultSortDirection(DiscoverySortConfiguration searchSortConfiguration, String sortOrder) {
        if (Objects.nonNull(searchSortConfiguration.getSortFields()) &&
                !searchSortConfiguration.getSortFields().isEmpty()) {
            sortOrder = searchSortConfiguration.getSortFields().get(0).getDefaultSortOrder().name();
        }
        return sortOrder;
    }

    private String getDefaultSortField(DiscoverySortConfiguration searchSortConfiguration) {
        String sortBy;// Attempt to find the default one, if none found we use SCORE
        sortBy = "score";
        if (Objects.nonNull(searchSortConfiguration.getSortFields()) &&
                !searchSortConfiguration.getSortFields().isEmpty()) {
            DiscoverySortFieldConfiguration defaultSort = searchSortConfiguration.getSortFields().get(0);
            if (StringUtils.isBlank(defaultSort.getMetadataField())) {
                return sortBy;
            }
            sortBy = defaultSort.getMetadataField();
        }
        return sortBy;
    }

    private void configurePagination(Integer size, Long offset, DiscoverQuery queryArgs) {
        queryArgs.setMaxResults(size != null ? Math.min(pageSizeLimit, size) : pageSizeLimit);
        queryArgs.setStart(offset != null ? Math.toIntExact(offset) : 0);
    }

    private String getDsoType(String dsoType) throws IllegalArgumentException {
        for (IndexFactory indexFactory : indexableFactories) {
            if (StringUtils.equalsIgnoreCase(indexFactory.getType(), dsoType)) {
                return indexFactory.getType();
            }
        }
        throw new IllegalArgumentException(dsoType + " is not a valid DSpace Object type");
    }

    public void setIndexableFactories(List<IndexFactory> indexableFactories) {
        this.indexableFactories = indexableFactories;
    }

    private DiscoverQuery addFaceting(Context context, IndexableObject scope, DiscoverQuery queryArgs,
                                      DiscoveryConfiguration discoveryConfiguration) {

        List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        log.debug("facets for configuration " + discoveryConfiguration.getId() + ": " + (facets != null ? facets
                .size() : null));

        if (facets != null) {
            queryArgs.setFacetMinCount(1);

            /** enable faceting of search results */
            for (DiscoverySearchFilterFacet facet : facets) {
                fillFacetIntoQueryArgs(context, scope, null, queryArgs, facet, facet.getFacetLimit());
            }
        }

        return queryArgs;
    }

    private String[] convertFiltersToString(Context context, DiscoveryConfiguration discoveryConfiguration,
                                            List<QueryBuilderSearchFilter> searchFilters)
            throws IllegalArgumentException {
        ArrayList<String> filterQueries = new ArrayList<>(CollectionUtils.size(searchFilters));

        try {
            for (QueryBuilderSearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
                DiscoverySearchFilter filter = discoveryConfiguration.getSearchFilter(searchFilter.getName());
                if (filter == null) {
                    throw new IllegalArgumentException(searchFilter.getName() + " is not a valid search filter");
                }

                DiscoverFilterQuery filterQuery = searchService.toFilterQuery(context,
                                                                              filter.getIndexFieldName(),
                                                                              searchFilter.getOperator(),
                                                                              searchFilter.getValue(),
                                                                              discoveryConfiguration);

                if (filterQuery != null) {
                    filterQueries.add(filterQuery.getFilterQuery());
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("There was a problem parsing the search filters.", e);
        }

        return filterQueries.toArray(new String[filterQueries.size()]);
    }


}
