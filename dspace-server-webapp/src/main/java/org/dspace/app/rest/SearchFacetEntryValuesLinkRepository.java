/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.parameter.resolver.SearchFilterResolverUtil;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
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
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(SearchFacetEntryRest.CATEGORY + "." + SearchFacetEntryRest.PLURAL_NAME + "." + SearchFacetEntryRest.VALUES)
public class SearchFacetEntryValuesLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {
    @Autowired
    private ScopeResolver scopeResolver;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private RestDiscoverQueryBuilder queryBuilder;

    @PreAuthorize("permitAll()")
    public Page<SearchFacetValueRest> getValues(@Nullable HttpServletRequest request, String facetName,
                                                @Nullable Pageable optionalPageable, Projection projection)
        throws SearchServiceException {
        Context context = obtainContext();

        String scope = request.getParameter("scope");
        String configuration = request.getParameter("configuration");
        String prefix = request.getParameter("prefix");
        String query = request.getParameter("query");
        List<SearchFilter> searchFilters = SearchFilterResolverUtil.resolveSearchFilters(request);
        String[] dsoTypeValues = request.getParameterValues("dsoType");
        List<String> dsoTypes = List.of();
        if (dsoTypeValues != null) {
            dsoTypes = List.of(dsoTypeValues);
        }

        IndexableObject scopeObject = scopeResolver.resolveScope(context, scope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrIndexableObject(context, configuration, scopeObject);

        DiscoverQuery discoverQuery = queryBuilder.buildFacetQuery(context, scopeObject, discoveryConfiguration, prefix,
            query, searchFilters, dsoTypes, optionalPageable, facetName);
        DiscoverResult searchResult = searchService.search(context, scopeObject, discoverQuery);

        DiscoverySearchFilterFacet field = discoveryConfiguration.getSidebarFacet(facetName);
        List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);

        return converter.toRestPage(facetValues, optionalPageable, projection);
    }
}
