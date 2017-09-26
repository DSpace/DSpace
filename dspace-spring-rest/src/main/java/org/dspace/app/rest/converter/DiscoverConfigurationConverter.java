package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by raf on 22/09/2017.
 *
 * TODO RAF UNIT TEST
 */
@Component
public class DiscoverConfigurationConverter {
    public SearchConfigurationRest convert(DiscoveryConfiguration configuration){
        SearchConfigurationRest searchConfigurationRest = new SearchConfigurationRest();
        addSearchFilters(searchConfigurationRest, configuration.getSearchFilters());
        addSortOptions(searchConfigurationRest, configuration.getSearchSortConfiguration());
        return searchConfigurationRest;
    }


    public void addSearchFilters(SearchConfigurationRest searchConfigurationRest, List<DiscoverySearchFilter> searchFilterList){
        for(DiscoverySearchFilter discoverySearchFilter : searchFilterList){
            SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
            filter.setFilter(discoverySearchFilter.getIndexFieldName());
            filter.addDefaultOperatorsToList();
            searchConfigurationRest.addFilter(filter);


        }
    }

    private void addSortOptions(SearchConfigurationRest searchConfigurationRest, DiscoverySortConfiguration searchSortConfiguration) {
        for(DiscoverySortFieldConfiguration discoverySearchSortConfiguration : searchSortConfiguration.getSortFields()){
            SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
            sortOption.setMetadata(discoverySearchSortConfiguration.getMetadataField());

            sortOption.setName(discoverySearchSortConfiguration.getType());

            searchConfigurationRest.addSortOption(sortOption);
        }
    }

}
