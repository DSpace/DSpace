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
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.DiscoverResultConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.parameter.resolver.SearchFilterResolverUtil;
import org.dspace.app.rest.utils.RestDiscoverQueryBuilder;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * RestRepository for the {@link SearchResultsRest} object
 */
@Component(DiscoveryConfigurationRest.CATEGORY + "." + SearchResultsRest.PLURAL_NAME)
public class SearchResultsRestRepository extends DSpaceRestRepository<SearchResultsRest, String> {
    private static final Logger log = LogManager.getLogger();

    private static final String SOLR_PARSE_ERROR_CLASS = "org.apache.solr.search.SyntaxError";

    @Autowired
    protected Utils utils;

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

    @SearchRestMethod(name = "objects")
    public SearchResultsRest searchObjects(
        @Parameter(value = "query") String query,
        @Parameter(value = "dsoType") List<String> dsoTypes,
        @Parameter(value = "scope") String scope,
        @Parameter(value = "configuration") String configuration,
        @Parameter(value = "hitHighlighting") Boolean hitHighlighting,
        Pageable page
    ) {
        List<SearchFilter> searchFilters = SearchFilterResolverUtil
            .resolveSearchFilters(requestService.getCurrentRequest().getHttpServletRequest());

        Context context = obtainContext();
        IndexableObject scopeObject = scopeResolver.resolveScope(context, scope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrIndexableObject(context, configuration, scopeObject);

        DiscoverResult searchResult;
        DiscoverQuery discoverQuery;

        try {
            discoverQuery = queryBuilder
                .buildQuery(context, scopeObject, discoveryConfiguration, query, searchFilters, dsoTypes, page,
                    hitHighlighting == null || hitHighlighting);
            searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            boolean isParsingException = e.getMessage().contains(SOLR_PARSE_ERROR_CLASS);
            if (isParsingException) {
                throw new UnprocessableEntityException(e.getMessage());
            } else {
                throw new IllegalArgumentException("Error while searching with Discovery: " + e.getMessage());
            }
        }

        return discoverResultConverter
            .convert(context, query, dsoTypes, configuration, scope, searchFilters, page, searchResult,
                discoveryConfiguration, utils.obtainProjection());
    }

    @Override
    public SearchResultsRest findOne(Context context, String s) {
        throw new RepositoryMethodNotImplementedException(SearchResultsRest.NAME, "findOne");
    }

    @Override
    public Page<SearchResultsRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(SearchResultsRest.NAME, "findAll");
    }

    @Override
    public Class<SearchResultsRest> getDomainClass() {
        return SearchResultsRest.class;
    }
}
