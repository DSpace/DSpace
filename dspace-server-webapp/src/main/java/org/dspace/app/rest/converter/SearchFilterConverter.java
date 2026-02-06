/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.SearchConfigInformation;
import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter to convert from {@link DiscoverySearchFilter} objects to {@link SearchFilterRest} objects.
 */
@Component
public class SearchFilterConverter implements DSpaceConverter<DiscoverySearchFilter, SearchFilterRest> {

    @Autowired
    private RequestService requestService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Override
    public SearchFilterRest convert(DiscoverySearchFilter filter, Projection projection) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        SearchConfigInformation information = SearchConfigInformation.fromRequest(request);
        DiscoveryConfiguration configuration = information
            .getConfiguration(ContextUtil.obtainContext(request), scopeResolver, searchConfigurationService);
        List<DiscoverySearchFilterFacet> facets = configuration.getSidebarFacets();

        SearchFilterRest searchFilterRest = new SearchFilterRest();

        searchFilterRest.setFilter(filter.getIndexFieldName());
        searchFilterRest.setFilterType(filter.getType());
        searchFilterRest.setOpenByDefault(filter.isOpenByDefault());
        searchFilterRest.setHasFacets(
                facets.stream().anyMatch(f -> f.getIndexFieldName().equals(filter.getIndexFieldName()))
        );
        searchFilterRest.setPageSize(filter.getPageSize());
        searchFilterRest.addDefaultOperatorsToList();
        searchFilterRest.setProjection(projection);

        return searchFilterRest;
    }

    @Override
    public Class<DiscoverySearchFilter> getModelClass() {
        return DiscoverySearchFilter.class;
    }
}
