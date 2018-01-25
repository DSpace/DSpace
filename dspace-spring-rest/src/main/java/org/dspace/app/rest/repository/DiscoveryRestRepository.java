/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.DiscoverConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverFacetConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverFacetResultsConverter;
import org.dspace.app.rest.converter.DiscoverFacetsConverter;
import org.dspace.app.rest.converter.DiscoverResultConverter;
import org.dspace.app.rest.converter.DiscoverSearchSupportConverter;
import org.dspace.app.rest.exception.InvalidRequestException;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.utils.DiscoverQueryBuilder;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to return a REST object to the controller class. This repository handles all the information lookup
 * that has to be done for the endpoint
 */
@Component(SearchResultsRest.CATEGORY + "." + SearchResultsRest.NAME)
public class DiscoveryRestRepository extends AbstractDSpaceRestRepository {

    private static final Logger log = Logger.getLogger(ScopeResolver.class);

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Autowired
    private DiscoverQueryBuilder queryBuilder;

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

    public SearchConfigurationRest getSearchConfiguration(final String dsoScope, final String configurationName) {
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        return discoverConfigurationConverter.convert(configuration);
    }

    public SearchResultsRest getSearchObjects(final String query, final String dsoType, final String dsoScope, final String configurationName,
                                              final List<SearchFilter> searchFilters, final Pageable page) throws InvalidRequestException {
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;

        try {
            discoverQuery = queryBuilder.buildQuery(context, scopeObject, configuration, query, searchFilters, dsoType, page);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
        }

        return discoverResultConverter.convert(context, query, dsoType, configurationName, dsoScope, searchFilters, page, searchResult, configuration);
    }

    public FacetConfigurationRest getFacetsConfiguration(final String dsoScope, final String configurationName) {
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        return discoverFacetConfigurationConverter.convert(configurationName, dsoScope, configuration);
    }

    public SearchSupportRest getSearchSupport() {
        return discoverSearchSupportConverter.convert();
    }

    public FacetResultsRest getFacetObjects(String facetName, String query, String dsoType, String dsoScope, List<SearchFilter> searchFilters, Pageable page) throws InvalidRequestException {

        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(facetName, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;
        try {
            discoverQuery = queryBuilder.buildFacetQuery(context, scopeObject, configuration, query, searchFilters, dsoType, page, facetName);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            //TODO TOM handle search exception
        }

        FacetResultsRest facetResultsRest = discoverFacetResultsConverter.convert(context, facetName, query, dsoType, dsoScope, searchFilters, searchResult, configuration, page);
        return facetResultsRest;
    }

    public SearchResultsRest getAllFacets(String query, String dsoType, String dsoScope, String configurationName, List<SearchFilter> searchFilters) throws InvalidRequestException{

        Context context = obtainContext();
        Pageable page = new PageRequest(1,1);
        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;

        try {
            discoverQuery = queryBuilder.buildQuery(context, scopeObject, configuration, query, searchFilters, dsoType, page);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
        }

        SearchResultsRest searchResultsRest = discoverFacetsConverter.convert(context, query, dsoType, configurationName, dsoScope, searchFilters, page, configuration, searchResult);

        return searchResultsRest;

    }
}
