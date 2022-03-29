/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

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
import org.dspace.app.rest.converter.query.SearchQueryConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.InvalidSearchRequestException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * This class builds the queries for the /search and /facet endpoints.
 */
@Component
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
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param query                     the query string for this discovery query
     * @param searchFilters             the search filters for this discovery query
     * @param dsoType                   only include search results with this type
     * @param page                      the pageable for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    String dsoType, Pageable page)
        throws DSpaceBadRequestException {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildQuery(context, scope, discoveryConfiguration, query, searchFilters, dsoTypes, page);
    }

    /**
     * Build a discovery query
     *
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param query                     the query string for this discovery query
     * @param searchFilters             the search filters for this discovery query
     * @param dsoTypes                  only include search results with one of these types
     * @param page                      the pageable for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    List<String> dsoTypes, Pageable page)
        throws DSpaceBadRequestException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                dsoTypes);

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting
        configurePagination(page, queryArgs);
        configureSorting(page, queryArgs, discoveryConfiguration.getSearchSortConfiguration());

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
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param prefix                    limit the facets results to those starting with the given prefix.
     * @param query                     the query string for this discovery query
     * @param searchFilters             the search filters for this discovery query
     * @param dsoType                   only include search results with this type
     * @param page                      the pageable for this discovery query
     * @param facetName                 the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         String dsoType, Pageable page, String facetName)
        throws DSpaceBadRequestException {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildFacetQuery(
                context, scope, discoveryConfiguration, prefix, query, searchFilters, dsoTypes, page, facetName);
    }

    /**
     * Create a discovery facet query.
     *
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param prefix                    limit the facets results to those starting with the given prefix.
     * @param query                     the query string for this discovery query
     * @param searchFilters             the search filters for this discovery query
     * @param dsoTypes                  only include search results with one of these types
     * @param page                      the pageable for this discovery query
     * @param facetName                 the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         List<String> dsoTypes, Pageable page, String facetName)
        throws DSpaceBadRequestException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                                                           dsoTypes);

        //When all search criteria are set, configure facet results
        addFacetingForFacets(context, scope, prefix, queryArgs, discoveryConfiguration, facetName, page);

        //We don' want any search results, we only want facet values
        queryArgs.setMaxResults(0);

        //Configure pagination
        configurePaginationForFacets(page, queryArgs);

        return queryArgs;
    }

    private void configurePaginationForFacets(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setFacetOffset(Math.toIntExact(page.getOffset()));
        }
    }

    private DiscoverQuery addFacetingForFacets(Context context, IndexableObject scope, String prefix,
            DiscoverQuery queryArgs, DiscoveryConfiguration discoveryConfiguration, String facetName, Pageable page)
            throws DSpaceBadRequestException {

        DiscoverySearchFilterFacet facet = discoveryConfiguration.getSidebarFacet(facetName);
        if (facet != null) {
            queryArgs.setFacetMinCount(1);
            int pageSize = Math.min(pageSizeLimit, page.getPageSize());

            fillFacetIntoQueryArgs(context, scope, prefix, queryArgs, facet, pageSize);

        } else {
            throw new DSpaceBadRequestException(facetName + " is not a valid search facet");
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
                    facet.getSortOrderSidebar(), StringUtils.trimToNull(prefix)));
        }
    }

    private DiscoverQuery buildCommonDiscoverQuery(Context context, DiscoveryConfiguration discoveryConfiguration,
                                                   String query,
                                                   List<SearchFilter> searchFilters, List<String> dsoTypes)
        throws DSpaceBadRequestException {
        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration);

        //Add search filters
        queryArgs.addFilterQueries(convertFilters(context, discoveryConfiguration, searchFilters));

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
                                                             new String[discoveryConfiguration.getDefaultFilterQueries()
                                                                                              .size()]));
        return queryArgs;
    }

    private void configureSorting(Pageable page, DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration) throws DSpaceBadRequestException {
        String sortBy = null;
        String sortOrder = null;

        //Read the Pageable object if there is one
        if (page != null) {
            Sort sort = page.getSort();
            if (sort != null && sort.iterator().hasNext()) {
                Sort.Order order = sort.iterator().next();
                sortBy = order.getProperty();
                sortOrder = order.getDirection().name();
            }
        }

        if (StringUtils.isNotBlank(sortBy) && !isConfigured(sortBy, searchSortConfiguration)) {
            throw new InvalidSearchRequestException(
                         "The field: " + sortBy + "is not configured for the configuration!");
        }

        //Load defaults if we did not receive values
        if (sortBy == null) {
            sortBy = getDefaultSortField(searchSortConfiguration);
        }
        if (sortOrder == null) {
            sortOrder = getDefaultSortDirection(searchSortConfiguration, sortOrder);
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
                throw new DSpaceBadRequestException(sortOrder + " is not a valid sort order");
            }

        } else {
            throw new DSpaceBadRequestException(sortBy + " is not a valid sort field");
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

    private void configurePagination(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setMaxResults(Math.min(pageSizeLimit, page.getPageSize()));
            queryArgs.setStart(Math.toIntExact(page.getOffset()));
        } else {
            queryArgs.setMaxResults(pageSizeLimit);
            queryArgs.setStart(0);
        }
    }

    private String getDsoType(String dsoType) throws DSpaceBadRequestException {
        for (IndexFactory indexFactory : indexableFactories) {
            if (StringUtils.equalsIgnoreCase(indexFactory.getType(), dsoType)) {
                return indexFactory.getType();
            }
        }
        throw new DSpaceBadRequestException(dsoType + " is not a valid DSpace Object type");
    }

    public void setIndexableFactories(List<IndexFactory> indexableFactories) {
        this.indexableFactories = indexableFactories;
    }

    private String[] convertFilters(Context context, DiscoveryConfiguration discoveryConfiguration,
                                    List<SearchFilter> searchFilters) throws DSpaceBadRequestException {
        ArrayList<String> filterQueries = new ArrayList<>(CollectionUtils.size(searchFilters));

        SearchQueryConverter searchQueryConverter = new SearchQueryConverter();
        List<SearchFilter> transformedFilters = searchQueryConverter.convert(searchFilters);
        try {
            for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(transformedFilters)) {
                DiscoverySearchFilter filter = discoveryConfiguration.getSearchFilter(searchFilter.getName());
                if (filter == null) {
                    throw new DSpaceBadRequestException(searchFilter.getName() + " is not a valid search filter");
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
            throw new DSpaceBadRequestException("There was a problem parsing the search filters.", e);
        }

        return filterQueries.toArray(new String[filterQueries.size()]);
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

}
