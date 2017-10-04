/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.DiscoverConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverFacetConfigurationConverter;
import org.dspace.app.rest.converter.DiscoverResultConverter;
import org.dspace.app.rest.converter.DiscoverSearchSupportConverter;
import org.dspace.app.rest.exception.InvalidRequestException;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TODO TOM UNIT TEST
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

    public SearchConfigurationRest getSearchConfiguration(final String dsoScope, final String configurationName) {
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        return discoverConfigurationConverter.convert(configuration);
    }

    public SearchResultsRest getSearchObjects(final String query, final String dsoType, final String dsoScope, final String configurationName, final List<SearchFilter> searchFilters, final Pageable page) {
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;

        try {
            discoverQuery = queryBuilder.buildQuery(context, scopeObject, configuration, query, searchFilters, dsoType, page);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

            //TODO set "hasMore" property on facets

        } catch (InvalidRequestException e) {
            log.warn("Received an invalid request", e);
            //TODO TOM handle invalid request
        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            //TODO TOM handle search exception
        }

        return discoverResultConverter.convert(context, discoverQuery, configurationName, dsoScope, searchFilters, page, searchResult, configuration);
    }

    public FacetConfigurationRest getFacetsConfiguration(final String dsoScope, final String configurationName) {
        //TODO
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);


        return discoverFacetConfigurationConverter.convert(configuration);
        //TODO Call DiscoveryConfigurationConverter on configuration to convert this API model to the REST model

        //TODO Return REST model
    }

    public SearchSupportRest getSearchSupport() {
        return discoverSearchSupportConverter.convert();
    }
}
