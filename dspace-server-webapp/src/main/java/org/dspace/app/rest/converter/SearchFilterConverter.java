/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.stereotype.Component;

/**
 * Converter to convert from {@link DiscoverySearchFilter} objects to {@link SearchFilterRest} objects.
 */
@Component
public class SearchFilterConverter implements DSpaceConverter<DiscoverySearchFilter, SearchFilterRest> {

    @Override
    public SearchFilterRest convert(DiscoverySearchFilter filter, Projection projection) {
        SearchFilterRest searchFilterRest = new SearchFilterRest();

        DiscoveryConfiguration defaultConfig =
            DSpaceServicesFactory.getInstance()
                .getServiceManager()
                .getServiceByName("defaultConfiguration", DiscoveryConfiguration.class);

        List<DiscoverySearchFilterFacet> sidebarFacets = defaultConfig.getSidebarFacets();

        searchFilterRest.setFilter(filter.getIndexFieldName());
        searchFilterRest.setFilterType(filter.getFilterType());
        searchFilterRest.setType(filter.getType());
        searchFilterRest.setOpenByDefault(filter.isOpenByDefault());
        // if in default configuration's sidebar facets -> hasFacets = true
        if (sidebarFacets.stream().anyMatch(f -> f.equals(filter))) {
            searchFilterRest.setHasFacets(true);
        }
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
