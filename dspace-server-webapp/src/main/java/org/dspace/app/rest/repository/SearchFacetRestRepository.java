/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetInformation;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(SearchFacetEntryRest.CATEGORY + "." + SearchFacetEntryRest.PLURAL_NAME)
public class SearchFacetRestRepository extends DSpaceRestRepository<SearchFacetEntryRest, String> {
    @Autowired
    protected Utils utils;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Override
    @PreAuthorize("permitAll()")
    public SearchFacetEntryRest findOne(Context context, String facetName) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        SearchFacetInformation information = SearchFacetInformation.fromRequest(request, facetName);
        DiscoveryConfiguration discoveryConfiguration =
            getConfiguration(context, information.getConfiguration(), information.getScope());

        SearchFacetEntryRest facet =
            converter.toRest(discoveryConfiguration.getSidebarFacet(facetName), utils.obtainProjection());
        facet.setFacetInformation(information);
        return facet;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<SearchFacetEntryRest> findAll(Context context, Pageable pageable) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        SearchFacetInformation information = SearchFacetInformation.fromRequest(request);
        DiscoveryConfiguration discoveryConfiguration =
            getConfiguration(context, information.getConfiguration(), information.getScope());

        Page<SearchFacetEntryRest> page =
            converter.toRestPage(discoveryConfiguration.getSidebarFacets(), pageable, utils.obtainProjection());
        page.getContent().forEach((entry) -> {
            entry.setFacetInformation(information);
        });
        return page;
    }

    private DiscoveryConfiguration getConfiguration(Context context, String configuration, String dsoScope) {
        IndexableObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
            .getDiscoveryConfigurationByNameOrIndexableObject(context, configuration, scopeObject);
        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(configuration);
        }
        return discoveryConfiguration;
    }

    @Override
    public Class<SearchFacetEntryRest> getDomainClass() {
        return SearchFacetEntryRest.class;
    }
}
