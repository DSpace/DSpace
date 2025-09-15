/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.model.SortOptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryConfigurationRestConverter
    implements DSpaceConverter<DiscoveryConfiguration, DiscoveryConfigurationRest> {

    @Override
    public DiscoveryConfigurationRest convert(DiscoveryConfiguration configuration, Projection projection) {
        DiscoveryConfigurationRest searchConfigurationRest = new DiscoveryConfigurationRest();
        searchConfigurationRest.setProjection(projection);
        if (configuration != null) {
            addSearchFilters(searchConfigurationRest,
                configuration.getSearchFilters(), configuration.getSidebarFacets());
            addSortOptions(searchConfigurationRest, configuration.getSearchSortConfiguration());
        }
        return searchConfigurationRest;
    }

    public Class<DiscoveryConfiguration> getModelClass() {
        return DiscoveryConfiguration.class;
    }


    public void addSearchFilters(DiscoveryConfigurationRest searchConfigurationRest,
                                 List<DiscoverySearchFilter> searchFilterList,
                                 List<DiscoverySearchFilterFacet> facetList) {
        List<String> facetFieldNames = facetList.stream().map(DiscoverySearchFilterFacet::getIndexFieldName)
            .collect(Collectors.toList());
        for (DiscoverySearchFilter discoverySearchFilter : CollectionUtils.emptyIfNull(searchFilterList)) {
            SearchFilterRest filter = new SearchFilterRest();
            filter.setFilter(discoverySearchFilter.getIndexFieldName());
            if (facetFieldNames.stream().anyMatch(str -> str.equals(discoverySearchFilter.getIndexFieldName()))) {
                filter.setHasFacets(true);
            }
            filter.setType(discoverySearchFilter.getType());
            filter.setOpenByDefault(discoverySearchFilter.isOpenByDefault());
            filter.addDefaultOperatorsToList();
            filter.setPageSize(discoverySearchFilter.getPageSize());
            searchConfigurationRest.addFilter(filter);
        }
    }

    private void addSortOptions(DiscoveryConfigurationRest searchConfigurationRest,
                                DiscoverySortConfiguration searchSortConfiguration) {
        if (searchSortConfiguration != null) {
            for (DiscoverySortFieldConfiguration discoverySearchSortConfiguration : CollectionUtils
                .emptyIfNull(searchSortConfiguration.getSortFields())) {
                SortOptionRest sortOption = new SortOptionRest();
                if (StringUtils.isBlank(discoverySearchSortConfiguration.getMetadataField())) {
                    sortOption.setName(DiscoverySortConfiguration.SCORE);
                } else {
                    sortOption.setName(discoverySearchSortConfiguration.getMetadataField());
                }
                sortOption.setActualName(discoverySearchSortConfiguration.getType());
                sortOption.setSortOrder(discoverySearchSortConfiguration.getDefaultSortOrder().name());
                searchConfigurationRest.addSortOption(sortOption);
            }

            DiscoverySortFieldConfiguration defaultSortField = searchSortConfiguration.getDefaultSortField();
            if (defaultSortField != null) {
                SortOptionRest sortOption = new SortOptionRest();
                sortOption.setName(defaultSortField.getMetadataField());
                sortOption.setActualName(defaultSortField.getType());
                sortOption.setSortOrder(defaultSortField.getDefaultSortOrder().name());
                searchConfigurationRest.setDefaultSortOption(sortOption);
            }
        }

    }
}
