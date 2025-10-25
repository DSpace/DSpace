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
import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
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
        List<DiscoverySearchFilterFacet> facets = getDiscoveryConfiguration().getSidebarFacets();

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

    private DiscoveryConfiguration getDiscoveryConfiguration() {
        DiscoveryConfiguration discoveryConfiguration = null;

        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        Context context = ContextUtil.obtainContext(request);

        String path = request.getServletPath().isEmpty() ? request.getPathInfo() : request.getServletPath();
        String[] pathParts = path.split("/");
        String configId = pathParts[4];

        if (configId.equals("scope")) {
            String uuid = request.getParameter("uuid");
            IndexableObject scopeObject = scopeResolver.resolveScope(context, uuid);
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(context, scopeObject);
        } else {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(configId);
        }

        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration("default");
        }

        return discoveryConfiguration;
    }
}
