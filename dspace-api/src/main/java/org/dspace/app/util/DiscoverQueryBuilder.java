package org.dspace.app.util;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryRelatedItemConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFunctionConfiguration;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    String dsoType)
            throws Exception {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildQuery(context, scope, discoveryConfiguration, query, searchFilters, dsoTypes);
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
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    List<String> dsoTypes)
            throws Exception {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                dsoTypes, scope);

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting

        addDiscoveryHitHighlightFields(discoveryConfiguration, queryArgs);

        queryArgs.setScopeObject(scope);
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
     * @param facetName              the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         String dsoType, String facetName)
            throws Exception {

        List<String> dsoTypes = dsoType != null ? singletonList(dsoType) : emptyList();

        return buildFacetQuery(
                context, scope, discoveryConfiguration, prefix, query, searchFilters, dsoTypes, facetName);
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
     * @param facetName              the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         List<String> dsoTypes, String facetName)
            throws Exception {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                dsoTypes, scope);

        //When all search criteria are set, configure facet results
        addFacetingForFacets(context, scope, prefix, queryArgs, discoveryConfiguration, facetName);

        //We don' want any search results, we only want facet values
        queryArgs.setMaxResults(0);

        //Configure pagination

        return queryArgs;
    }

    private void configurePaginationForFacet(DiscoverQuery queryArgs) {

    }

    private DiscoverQuery addFacetingForFacets(Context context, IndexableObject scope, String prefix,
                                               DiscoverQuery queryArgs, DiscoveryConfiguration discoveryConfiguration, String facetName)
            throws Exception {

        DiscoverySearchFilterFacet facet = discoveryConfiguration.getSidebarFacet(facetName);
        if (facet != null) {
            queryArgs.setFacetMinCount(1);

        } else {
            throw new Exception(facetName + " is not a valid search facet");
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
                log.error(LogManager.getHeader(context, "Error in Discovery while setting up date facet range",
                        "date facet: " + facet), e);
            }

        } else {

            //Add one to our facet limit to make sure that if we have more then the shown facets that we show our
            // "show more" url
            int facetLimit = pageSize + 1;
            //This should take care of the sorting for us
            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit,
                    facet.getSortOrderSidebar(), StringUtils.trimToNull(prefix),
                    facet.exposeMore(), facet.exposeMissing(), facet.exposeTotalElements(), facet.fillDateGaps(),
                    facet.inverseDirection()));
        }
    }

    private DiscoverQuery buildCommonDiscoverQuery(Context context, DiscoveryConfiguration discoveryConfiguration,
                                                   String query,
                                                   List<SearchFilter> searchFilters, List<String> dsoTypes,
                                                   IndexableObject scope)
            throws Exception {
        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration, scope);
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

    private DiscoverQuery buildBaseQueryForConfiguration(
            DiscoveryConfiguration discoveryConfiguration, IndexableObject scope) {
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.setDiscoveryConfigurationName(discoveryConfiguration.getId());

        String[] queryArray = discoveryConfiguration.getDefaultFilterQueries()
                .toArray(
                        new String[discoveryConfiguration.getDefaultFilterQueries()
                                .size()]);

        if (discoveryConfiguration != null &&
                discoveryConfiguration instanceof DiscoveryRelatedItemConfiguration) {
            if (queryArray != null) {
                for (int i = 0; i < queryArray.length; i++) {
                    queryArray[i] = MessageFormat.format(queryArray[i], scope.getID());
                }
            } else {
                log.warn("you are trying to set queries parameters on an empty queries list");
            }
        }

        queryArgs.addFilterQueries(queryArray);
        return queryArgs;
    }

    private void configureSorting(DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration,
                                  final IndexableObject scope) throws Exception {
        String sortBy = null;
        String sortOrder = null;


        if (StringUtils.isNotBlank(sortBy) && !isConfigured(sortBy, searchSortConfiguration)) {
            throw new Exception(
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
            String sortField;

            if (DiscoverySortFunctionConfiguration.SORT_FUNCTION.equals(sortFieldConfiguration.getType())) {
                sortField = MessageFormat.format(
                        ((DiscoverySortFunctionConfiguration) sortFieldConfiguration).getFunction(scope.getID()),
                        scope.getID());
            } else {
                sortField = searchService
                        .toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
            }


            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
            } else {
                throw new Exception(sortOrder + " is not a valid sort order");
            }

        } else {
            throw new Exception(sortBy + " is not a valid sort field");
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


    private String getDsoType(String dsoType)   {
        for (IndexFactory indexFactory : indexableFactories) {
            if (StringUtils.equalsIgnoreCase(indexFactory.getType(), dsoType)) {
                return indexFactory.getType();
            }
        }
        return null;
//        throw new Exception(dsoType + " is not a valid DSpace Object type");
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

}
