/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to create a SearchConfigurationRest object from the DiscoveryConfiguration to be given
 * to the convert method.
 *
 */
@Component
public class DiscoverConfigurationConverter {
    public SearchConfigurationRest convert(DiscoveryConfiguration configuration){
        SearchConfigurationRest searchConfigurationRest = new SearchConfigurationRest();
        if(configuration != null){
            addSearchFilters(searchConfigurationRest, configuration.getSearchFilters());
            addSortOptions(searchConfigurationRest, configuration.getSearchSortConfiguration());
            setDefaultSortOption(configuration, searchConfigurationRest);
        }
        return searchConfigurationRest;
    }

    private void setDefaultSortOption(DiscoveryConfiguration configuration, SearchConfigurationRest searchConfigurationRest) {
        String defaultSort = configuration.getSearchSortConfiguration().SCORE;
        if(configuration.getSearchSortConfiguration() != null){
            DiscoverySortFieldConfiguration discoverySortFieldConfiguration = configuration.getSearchSortConfiguration().getSortFieldConfiguration(defaultSort);
            if(discoverySortFieldConfiguration != null){
                SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
                sortOption.setName(discoverySortFieldConfiguration.getMetadataField());
                sortOption.setActualName(discoverySortFieldConfiguration.getType());
                searchConfigurationRest.addSortOption(sortOption);
            }
        }
    }


    public void addSearchFilters(SearchConfigurationRest searchConfigurationRest, List<DiscoverySearchFilter> searchFilterList){
            for(DiscoverySearchFilter discoverySearchFilter : CollectionUtils.emptyIfNull(searchFilterList)){
                SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
                filter.setFilter(discoverySearchFilter.getIndexFieldName());
                filter.addDefaultOperatorsToList();
                searchConfigurationRest.addFilter(filter);
            }
    }

    private void addSortOptions(SearchConfigurationRest searchConfigurationRest, DiscoverySortConfiguration searchSortConfiguration) {
        if(searchSortConfiguration!=null){
            for(DiscoverySortFieldConfiguration discoverySearchSortConfiguration : CollectionUtils.emptyIfNull(searchSortConfiguration.getSortFields())){
                SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
                sortOption.setName(discoverySearchSortConfiguration.getMetadataField());
                sortOption.setActualName(discoverySearchSortConfiguration.getType());
                searchConfigurationRest.addSortOption(sortOption);
            }
        }

    }

}
