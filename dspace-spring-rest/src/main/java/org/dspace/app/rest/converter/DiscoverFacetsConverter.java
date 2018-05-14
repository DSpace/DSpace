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
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetsConverter {

    private DiscoverFacetValueConverter facetValueConverter = new DiscoverFacetValueConverter();

    @Autowired
    private SearchService searchService;

    public SearchResultsRest convert(Context context, String query, String dsoType, String configurationName,
                                     String dsoScope, List<SearchFilter> searchFilters, final Pageable page,
                                     DiscoveryConfiguration configuration, DiscoverResult searchResult) {

        SearchResultsRest searchResultsRest = new SearchResultsRest();

        setRequestInformation(context, query, dsoType, configurationName, dsoScope, searchFilters, page,
                              searchResultsRest);
        addFacetValues(context, searchResult, searchResultsRest, configuration);

        return searchResultsRest;
    }


    private void addFacetValues(Context context, final DiscoverResult searchResult,
                                final SearchResultsRest searchResultsRest,
                                final DiscoveryConfiguration configuration) {

        List<DiscoverySearchFilterFacet> facets = configuration.getSidebarFacets();
        for (DiscoverySearchFilterFacet field : CollectionUtils.emptyIfNull(facets)) {

            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(field.getIndexFieldName());
            int valueCount = 0;
            facetEntry.setFacetLimit(field.getFacetLimit());
            facetEntry.setExposeMinMax(field.isExposeMinMax());
            if (field.isExposeMinMax()) {
                try {
                    calculateMinMaxValues(context, facetEntry, field);
                } catch (SearchServiceException e) {
                    e.printStackTrace();
                }
            }
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

    private void calculateMinMaxValues(Context context,SearchFacetEntryRest facetEntry,DiscoverySearchFilterFacet field)
        throws SearchServiceException {

        //TODO Move to searchService, only call to retrieve values (split logic)
        int oldestYear = 0;
        int newestYear = 0;

        DiscoverQuery minQuery = new DiscoverQuery();
        minQuery.setMaxResults(1);
        //Set our query to anything that has this value
        String indexFieldName = field.getIndexFieldName();
        minQuery.addFieldPresentQueries(indexFieldName + "_min");
        //Set sorting so our last value will appear on top
        minQuery.setSortField(indexFieldName + "_min_sort",DiscoverQuery.SORT_ORDER.asc);
        minQuery.addSearchField(indexFieldName + "_min");
        DiscoverResult minResult = searchService.search(context, minQuery);

        if (0 < minResult.getDspaceObjects().size()) {
            List<DiscoverResult.SearchDocument> searchDocuments = minResult
                .getSearchDocument(minResult.getDspaceObjects().get(0));
            if (0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues
                                                                     (indexFieldName).size()) {
//                oldestYear = Integer.parseInt(searchDocuments.get(0)
//                                                             .getSearchFieldValues(indexFieldName).get(0));
            }
        }
        //Now get the first year

        DiscoverQuery maxQuery = new DiscoverQuery();
        maxQuery.setMaxResults(1);
        //Set our query to anything that has this value
        indexFieldName = field.getIndexFieldName();
        maxQuery.addFieldPresentQueries(indexFieldName + "_max");
        //Set sorting so our last value will appear on top
        maxQuery.setSortField(indexFieldName + "_max_sort",DiscoverQuery.SORT_ORDER.desc);
        maxQuery.addSearchField(indexFieldName + "_max");
        DiscoverResult maxResult = searchService.search(context, maxQuery);
        if (0 < maxResult.getDspaceObjects().size()) {
            List<DiscoverResult.SearchDocument> searchDocuments = maxResult
                .getSearchDocument(maxResult.getDspaceObjects().get(0));
            if (0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues
                                                                        (indexFieldName).size()) {
//                newestYear = Integer.parseInt(searchDocuments.get(0).getSearchFieldValues
//                                                                    (indexFieldName).get(0));
            }
        }



    }

    private void setRequestInformation(final Context context, final String query, final String dsoType,
                                       final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page,
                                       final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfigurationName(configurationName);
        resultsRest.setDsoType(dsoType);
        resultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        resultsRest.setScope(scope);

        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new
            SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest
                .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
