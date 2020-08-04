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
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to construct a FacetResultsRest object from a number of parameters in the convert method
 */
@Component
public class DiscoverFacetResultsConverter {
    @Autowired
    private DiscoverFacetValueConverter facetValueConverter;

    @Autowired
    private SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter;

    public FacetResultsRest convert(Context context, String facetName, String prefix, String query, String dsoType,
                                    String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult,
                                    DiscoveryConfiguration configuration, Pageable page, Projection projection) {
        FacetResultsRest facetResultsRest = new FacetResultsRest();
        facetResultsRest.setProjection(projection);

        setRequestInformation(context, facetName, prefix, query, dsoType, dsoScope, searchFilters, searchResult,
                configuration, facetResultsRest, page, projection);

        addToFacetResultList(facetName, searchResult, facetResultsRest, configuration, page, projection);

        return facetResultsRest;
    }

    private void addToFacetResultList(String facetName, DiscoverResult searchResult, FacetResultsRest facetResultsRest,
                                      DiscoveryConfiguration configuration, Pageable page, Projection projection) {

        DiscoverySearchFilterFacet field = configuration.getSidebarFacet(facetName);
        List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);
        int valueCount = 0;

        for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
            if (valueCount >= page.getPageSize()) {
                //We requested one facet value more as the page size. We must make sure to not return the extra value.
                break;
            }
            SearchFacetValueRest searchFacetValueRest = buildSearchFacetValueRestFromFacetResult(value, projection);
            facetResultsRest.addToFacetResultList(searchFacetValueRest);
            valueCount++;
        }
    }

    private SearchFacetValueRest buildSearchFacetValueRestFromFacetResult(DiscoverResult.FacetResult value,
                                                                          Projection projection) {
        return facetValueConverter.convert(value, projection);
    }

    private void setRequestInformation(Context context, String facetName, String prefix, String query, String dsoType,
                                       String dsoScope, List<SearchFilter> searchFilters, DiscoverResult searchResult,
                                       DiscoveryConfiguration configuration, FacetResultsRest facetResultsRest,
                                       Pageable page, Projection projection) {
        facetResultsRest.setQuery(query);
        facetResultsRest.setPrefix(prefix);
        facetResultsRest.setScope(dsoScope);
        facetResultsRest.setDsoType(dsoType);

        facetResultsRest.setFacetEntry(convertFacetEntry(facetName, searchResult, configuration, page, projection));

        facetResultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        facetResultsRest.setSearchFilters(searchFilters);

        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            facetResultsRest
                .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }

    private SearchFacetEntryRest convertFacetEntry(final String facetName, final DiscoverResult searchResult,
                                                   final DiscoveryConfiguration configuration, final Pageable page,
                                                   final Projection projection) {
        DiscoverySearchFilterFacet field = configuration.getSidebarFacet(facetName);

        SearchFacetEntryRest facetEntryRest = new SearchFacetEntryRest(facetName);
        facetEntryRest.setProjection(projection);
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
