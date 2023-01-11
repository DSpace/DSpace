/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.DiscoverConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverFacetConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverFacetResultsConverter;
import org.dspace.app.rest.converter.DiscoverFacetsConverter;
import org.dspace.app.rest.converter.DiscoverResultConverter;
import org.dspace.app.rest.converter.DiscoverSearchSupportConverter;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.RestDiscoverQueryBuilder;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to return a REST object to the controller class. This repository handles all the
 * information lookup
 * that has to be done for the endpoint
 */
@Component(SearchResultsRest.CATEGORY + "." + SearchResultsRest.NAME)
public class DiscoveryRestRepository extends AbstractDSpaceRestRepository {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Autowired
    private RestDiscoverQueryBuilder queryBuilder;

    @Autowired
    private DiscoverResultConverter discoverResultConverter;

    @Autowired
    private DiscoverConfigurationConverter discoverConfigurationConverter;

    @Autowired
    private DiscoverFacetConfigurationConverter discoverFacetConfigurationConverter;

    @Autowired
    private DiscoverSearchSupportConverter discoverSearchSupportConverter;

    @Autowired
    private DiscoverFacetResultsConverter discoverFacetResultsConverter;

    @Autowired
    private DiscoverFacetsConverter discoverFacetsConverter;

    public SearchConfigurationRest getSearchConfiguration(final String dsoScope, final String configuration) {
        Context context = obtainContext();

        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        return discoverConfigurationConverter.convert(discoveryConfiguration, utils.obtainProjection());
    }

    public SearchResultsRest getSearchObjects(final String query, final List<String> dsoTypes, final String dsoScope,
                                              final String configuration,
                                              final List<SearchFilter> searchFilters, final Pageable page,
                                              final Projection projection) {
        Context context = obtainContext();
        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;

        try {
            discoverQuery = queryBuilder
                .buildQuery(context, scopeObject, discoveryConfiguration, query, searchFilters, dsoTypes, page);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            throw new IllegalArgumentException("Error while searching with Discovery: " + e.getMessage());
        }

        return discoverResultConverter
            .convert(context, query, dsoTypes, configuration, dsoScope, searchFilters, page, searchResult,
                     discoveryConfiguration, projection);
    }

    public FacetConfigurationRest getFacetsConfiguration(final String dsoScope, final String configuration) {
        Context context = obtainContext();

        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        return discoverFacetConfigurationConverter.convert(configuration, dsoScope, discoveryConfiguration);
    }

    public SearchSupportRest getSearchSupport() {
        return discoverSearchSupportConverter.convert();
    }

    public FacetResultsRest getFacetObjects(String facetName, String prefix, String query, List<String> dsoTypes,
            String dsoScope, final String configuration, List<SearchFilter> searchFilters, Pageable page)
                    throws SearchServiceException {

        Context context = obtainContext();

        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        DiscoverQuery discoverQuery = queryBuilder.buildFacetQuery(context, scopeObject, discoveryConfiguration, prefix,
                query, searchFilters, dsoTypes, page, facetName);
        DiscoverResult searchResult = searchService.search(context, scopeObject, discoverQuery);

        FacetResultsRest facetResultsRest = discoverFacetResultsConverter.convert(context, facetName, prefix, query,
                dsoTypes, dsoScope, searchFilters, searchResult, discoveryConfiguration, page,
                utils.obtainProjection());
        return facetResultsRest;
    }

    public SearchResultsRest getAllFacets(String query, List<String> dsoTypes, String dsoScope, String configuration,
                                          List<SearchFilter> searchFilters) {

        Context context = obtainContext();
        Pageable page = PageRequest.of(1, 1);
        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;

        try {
            discoverQuery = queryBuilder
                .buildQuery(context, scopeObject, discoveryConfiguration, query, searchFilters, dsoTypes, page);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
        }

        SearchResultsRest searchResultsRest = discoverFacetsConverter.convert(context, query, dsoTypes,
                configuration, dsoScope, searchFilters, page, discoveryConfiguration, searchResult,
                utils.obtainProjection());

        return searchResultsRest;

    }
}
