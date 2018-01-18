/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

import java.util.List;

@Component
public class DiscoverFacetsConverter {

    private DiscoverFacetValueConverter facetValueConverter = new DiscoverFacetValueConverter();


    public SearchResultsRest convert(Context context, String query, String dsoType, String configurationName, String dsoScope, List<SearchFilter> searchFilters, final Pageable page, DiscoveryConfiguration configuration, DiscoverResult searchResult) {

        SearchResultsRest searchResultsRest = new SearchResultsRest();

        setRequestInformation(context, query, dsoType, configurationName, dsoScope, searchFilters, page, searchResultsRest);
        addFacetValues(searchResult, searchResultsRest, configuration);

        return searchResultsRest;
    }


    private void addFacetValues(final DiscoverResult searchResult, final SearchResultsRest searchResultsRest, final DiscoveryConfiguration configuration) {

        List<DiscoverySearchFilterFacet> facets = configuration.getSidebarFacets();
        for (DiscoverySearchFilterFacet field : CollectionUtils.emptyIfNull(facets)) {

            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(field.getIndexFieldName());
            int valueCount = 0;
            facetEntry.setFacetLimit(field.getFacetLimit());

            for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
                //The discover results contains max facetLimit + 1 values. If we reach the "+1", indicate that there are
                //more results available.
                if (valueCount < field.getFacetLimit()) {
                    SearchFacetValueRest valueRest = facetValueConverter.convert(value);
                    facetEntry.addValue(valueRest);
                } else {
                    facetEntry.setHasMore(true);
                }

                if (StringUtils.isBlank(facetEntry.getFacetType())) {
                    facetEntry.setFacetType(value.getFieldType());
                }

                valueCount++;
            }

            searchResultsRest.addFacetEntry(facetEntry);
        }
    }

    private void setRequestInformation(final Context context, final String query, final String dsoType, final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page, final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfigurationName(configurationName);
        resultsRest.setDsoType(dsoType);
        resultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        resultsRest.setScope(scope);

        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
