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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.query.SearchQueryConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.InvalidSearchRequestException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * This class builds the queries for the /search and /facet endpoints.
 */
@Component
public class RestDiscoverQueryBuilder {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RestDiscoverQueryBuilder.class);

    @Autowired
    private DiscoverQueryBuilder discoverQueryBuilder;

    /**
     * Build a discovery query
     *
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoType                only include search results with this type
     * @param page                   the pageable for this discovery query
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
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoTypes               only include search results with one of these types
     * @param page                   the pageable for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    List<String> dsoTypes, Pageable page)
            throws DSpaceBadRequestException {

        try {
            List<QueryBuilderSearchFilter> transformedFilters = transformRestFilters(searchFilters);
            //Read the Pageable object if there is one
            if (page != null) {
                String sortBy = null;
                String sortOrder = null;

                Sort sort = page.getSort();
                if (sort != null && sort.iterator().hasNext()) {
                    Sort.Order order = sort.iterator().next();
                    sortBy = order.getProperty();
                    sortOrder = order.getDirection().name();
                }
                return discoverQueryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                                                       transformedFilters, dsoTypes, page.getPageSize(),
                                                       page.getOffset(), sortBy, sortOrder);
            } else {
                return discoverQueryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                                                       transformedFilters, dsoTypes, null, null, null, null);
            }
        } catch (IllegalArgumentException e) {
            throw new DSpaceBadRequestException(e.getMessage());
        } catch (SearchServiceException e) {
            throw new InvalidSearchRequestException(e.getMessage());
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
     * @param page                   the pageable for this discovery query
     * @param facetName              the facet field
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
     * @param context                the DSpace context
     * @param scope                  the scope for this discovery query
     * @param discoveryConfiguration the discovery configuration for this discovery query
     * @param prefix                 limit the facets results to those starting with the given prefix.
     * @param query                  the query string for this discovery query
     * @param searchFilters          the search filters for this discovery query
     * @param dsoTypes               only include search results with one of these types
     * @param page                   the pageable for this discovery query
     * @param facetName              the facet field
     */
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         List<String> dsoTypes, Pageable page, String facetName)
            throws DSpaceBadRequestException {

        try {
            List<QueryBuilderSearchFilter> transformedFilters = transformRestFilters(searchFilters);

            //Read the Pageable object if there is one
            if (page != null) {
                return discoverQueryBuilder
                        .buildFacetQuery(context, scope, discoveryConfiguration, prefix, query, transformedFilters,
                                         dsoTypes, page.getPageSize(), page.getOffset(), facetName);

            } else {
                return discoverQueryBuilder
                        .buildFacetQuery(context, scope, discoveryConfiguration, prefix, query, transformedFilters,
                                         dsoTypes, null, null, facetName);
            }
        } catch (IllegalArgumentException e) {
            throw new DSpaceBadRequestException(e.getMessage());
        }
    }

    private List<QueryBuilderSearchFilter> transformRestFilters(List<SearchFilter> searchFilters)
            throws DSpaceBadRequestException {
        SearchQueryConverter searchQueryConverter = new SearchQueryConverter();
        List<SearchFilter> filters = searchQueryConverter.convert(searchFilters);
        return filters.stream().map((searchFilter -> new QueryBuilderSearchFilter(searchFilter.getName(),
                                                                                  searchFilter.getOperator(),
                                                                                  searchFilter.getValue())))
                      .collect(Collectors.toList());
    }

}
