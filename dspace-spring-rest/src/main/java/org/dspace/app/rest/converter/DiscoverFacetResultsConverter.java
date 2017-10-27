package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetResultsConverter {

    private DiscoverFacetValueConverter facetValueConverter = new DiscoverFacetValueConverter();

    public FacetResultsRest convert(Context context, String facetName, String query, String dsoType, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, Pageable page){
        FacetResultsRest facetResultsRest = new FacetResultsRest();

        addToFacetResultList(facetName, searchResult, facetResultsRest, page);

        setRequestInformation(context, facetName, query, dsoType, dsoScope, searchFilters, searchResult, configuration, facetResultsRest, page);

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
        return facetValueConverter.convert(value);
    }

    private void setRequestInformation(Context context, String facetName, String query, String dsoType, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, FacetResultsRest facetResultsRest, Pageable page) {
        facetResultsRest.setQuery(query);
        facetResultsRest.setScope(dsoScope);
        facetResultsRest.setDsoType(dsoType);
        facetResultsRest.setPage(page);

        facetResultsRest.setFacetEntry(convertFacetEntry(facetName, searchResult, page));

        facetResultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        facetResultsRest.setSearchFilters(searchFilters);

        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            facetResultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }

    private SearchFacetEntryRest convertFacetEntry(final String facetName, final DiscoverResult searchResult, final Pageable page) {
        SearchFacetEntryRest facetEntryRest = new SearchFacetEntryRest(facetName);
        if(!searchResult.getFacetResult(facetName).isEmpty()){
            facetEntryRest.setFacetType(searchResult.getFacetResult(facetName).get(0).getFieldType());
        }

        if(searchResult.getFacetResult(facetName).size() > page.getPageSize()){
            //We requested one extra facet value. Check if that value is present to indicate that there are more results
            facetEntryRest.setHasMore(true);
        }

        return facetEntryRest;
    }
}
