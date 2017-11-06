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
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to construct a FacetResultsRest object from a number of parameters in the convert method
 */
@Component
public class DiscoverFacetResultsConverter {

    private DiscoverFacetValueConverter facetValueConverter = new DiscoverFacetValueConverter();

    public FacetResultsRest convert(Context context, String facetName, String query, String dsoType, String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult, DiscoveryConfiguration configuration, Pageable page) {
        FacetResultsRest facetResultsRest = new FacetResultsRest();

        setRequestInformation(context, facetName, query, dsoType, dsoScope, searchFilters, searchResult, configuration, facetResultsRest, page);

        addToFacetResultList(facetName, searchResult, facetResultsRest, configuration, page);

        return facetResultsRest;
    }

    private void addToFacetResultList(String facetName, DiscoverResult searchResult, FacetResultsRest facetResultsRest,
                                      DiscoveryConfiguration configuration, Pageable page) {

        DiscoverySearchFilterFacet field = configuration.getSidebarFacet(facetName);
        List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);
        int valueCount = 0;

        for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
            if (valueCount >= page.getPageSize()) {
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

        facetResultsRest.setFacetEntry(convertFacetEntry(facetName, searchResult, configuration, page));

        facetResultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        facetResultsRest.setSearchFilters(searchFilters);

        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            facetResultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }

    private SearchFacetEntryRest convertFacetEntry(final String facetName, final DiscoverResult searchResult, final DiscoveryConfiguration configuration, final Pageable page) {
        DiscoverySearchFilterFacet field = configuration.getSidebarFacet(facetName);

        SearchFacetEntryRest facetEntryRest = new SearchFacetEntryRest(facetName);
        List<DiscoverResult.FacetResult> facetResults = searchResult.getFacetResult(field);

        if (!facetResults.isEmpty()) {
            facetEntryRest.setFacetType(facetResults.get(0).getFieldType());
        }

        facetEntryRest.setFacetLimit(field.getFacetLimit());

        //We requested one extra facet value. Check if that value is present to indicate that there are more results
        facetEntryRest.setHasMore(facetResults.size() > page.getPageSize());

        return facetEntryRest;
    }
}
