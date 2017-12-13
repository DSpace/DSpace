package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetResultsConverter {


    public FacetResultsRest convert(Context context, String facetName, DiscoverQuery discoverQuery, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, Pageable page){
        FacetResultsRest facetResultsRest = new FacetResultsRest();

        addToFacetResultList(facetName, searchResult, facetResultsRest, page);

        setRequestInformation(context, facetName, discoverQuery, dsoScope, searchFilters, searchResult, configuration, facetResultsRest, page);

        return facetResultsRest;
    }

    private void addToFacetResultList(String facetName, DiscoverResult searchResult, FacetResultsRest facetResultsRest, Pageable page) {
            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(facetName);
            int valueCount = 0;
            for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
                if(valueCount >= page.getPageSize()){
                    //We requested one facet value more as the page size. We must make sure to not return the extra value.
                    break;
                }
                SearchFacetValueRest searchFacetValueRest = buildSearchFacetValueRestFromFacetResult(value);
                facetResultsRest.addToFacetResultList(searchFacetValueRest);
                valueCount++;
            }
    }

    private SearchFacetValueRest buildSearchFacetValueRestFromFacetResult(DiscoverResult.FacetResult value) {
        SearchFacetValueRest searchFacetValueRest = new SearchFacetValueRest();
        searchFacetValueRest.setLabel(value.getDisplayedValue());
        searchFacetValueRest.setCount(value.getCount());
        return searchFacetValueRest;
    }

    private void setRequestInformation(Context context, String facetName, DiscoverQuery discoverQuery, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, FacetResultsRest facetResultsRest, Pageable page) {
        facetResultsRest.setName(facetName);
        facetResultsRest.setQuery(discoverQuery.getQuery());
        facetResultsRest.setScope(dsoScope);
        facetResultsRest.setQuery(discoverQuery.getQuery());
        facetResultsRest.setPage(page);

        if(!searchResult.getFacetResult(facetName).isEmpty()){
            facetResultsRest.setType(searchResult.getFacetResult(facetName).get(0).getFieldType());
        }

        if(searchResult.getFacetResult(facetName).size() > page.getPageSize()){
            //We requested one extra facet value. Check if that value is present to indicate that there are more results
            facetResultsRest.setHasMore(true);
        }

        if(!discoverQuery.getFacetFields().isEmpty()){
            DiscoveryConfigurationParameters.SORT sort2 = discoverQuery.getFacetFields().get(0).getSortOrder();
            SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting(sort2.name());
            facetResultsRest.setSort(sort);
        }

        facetResultsRest.setSearchFilters(searchFilters);
        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            facetResultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
