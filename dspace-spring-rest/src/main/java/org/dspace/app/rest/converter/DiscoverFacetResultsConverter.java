package org.dspace.app.rest.converter;

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

import java.util.List;

@Component
public class DiscoverFacetResultsConverter {

    private static final int MAX_RESULTS = 10;

    public FacetResultsRest convert(Context context, String facetName, DiscoverQuery discoverQuery, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, Pageable page){
        FacetResultsRest facetResultsRest = new FacetResultsRest();
        addToFacetResultList(facetName, searchResult, facetResultsRest);
        setRequestInformation(context, facetName, discoverQuery, dsoScope, searchFilters, searchResult, configuration, facetResultsRest, page);

        return facetResultsRest;
    }

    private void addToFacetResultList(String facetName, DiscoverResult searchResult, FacetResultsRest facetResultsRest) {
            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(facetName);
            int valueCount = 0;
            for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
                if(valueCount >= MAX_RESULTS){
                    break;
                }
                SearchFacetValueRest valueRest = buildSearchFacetValueRestFromFacetResult(value);
                facetResultsRest.addToFacetResultList(valueRest);
                valueCount++;
            }
    }

    private SearchFacetValueRest buildSearchFacetValueRestFromFacetResult(DiscoverResult.FacetResult value) {
        SearchFacetValueRest valueRest = new SearchFacetValueRest();
        valueRest.setLabel(value.getDisplayedValue());
        valueRest.setFilterValue(value.getAsFilterQuery());
        valueRest.setFilterType(value.getFilterType());
        valueRest.setAuthorityKey(value.getAuthorityKey());
        valueRest.setSortValue(value.getSortValue());
        valueRest.setCount(value.getCount());
        return valueRest;
    }

    private void setRequestInformation(Context context, String facetName, DiscoverQuery discoverQuery, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, FacetResultsRest facetResultsRest, Pageable page) {
        facetResultsRest.setName(facetName);
        facetResultsRest.setQuery(discoverQuery.getQuery());
        facetResultsRest.setScope(dsoScope);
        facetResultsRest.setQuery(discoverQuery.getQuery());
        if(!searchResult.getFacetResult(facetName).isEmpty()){
            facetResultsRest.setType(searchResult.getFacetResult(facetName).get(0).getFieldType());
        }
        if(searchResult.getFacetResult(facetName).size() > MAX_RESULTS && searchResult.getFacetResult(facetName).get(MAX_RESULTS) != null){
            facetResultsRest.setHasMore(true);
        }
        if(!discoverQuery.getFacetFields().isEmpty()){
            DiscoveryConfigurationParameters.SORT sort2 = discoverQuery.getFacetFields().get(0).getSortOrder();
            SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting(sort2.name());
            facetResultsRest.setSort(sort);
        }
        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            facetResultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
