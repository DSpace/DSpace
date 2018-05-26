/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to create a SearchResultsRest object from the given parameters
 */
@Component
public class DiscoverResultConverter {

    @Autowired
    private List<BrowsableDSpaceObjectConverter> converters;
    @Autowired
    private DiscoverFacetsConverter facetConverter;
    @Autowired
    private SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter;

    public SearchResultsRest convert(final Context context, final String query, final String dsoType,
                                     final String configuration, final String scope,
                                     final List<SearchFilter> searchFilters, final Pageable page,
            final DiscoverResult searchResult, final DiscoveryConfiguration discoveryConfiguration) {

        SearchResultsRest resultsRest = new SearchResultsRest();

        setRequestInformation(context, query, dsoType, configuration, scope, searchFilters, page, resultsRest);

        addSearchResults(searchResult, resultsRest);

        addFacetValues(searchResult, resultsRest, discoveryConfiguration);

        resultsRest.setTotalNumberOfResults(searchResult.getTotalSearchResults());

        return resultsRest;
    }

    private void addFacetValues(final DiscoverResult searchResult, final SearchResultsRest resultsRest,
            final DiscoveryConfiguration configuration) {
        facetConverter.addFacetValues(searchResult, resultsRest, configuration);
    }

    private void addSearchResults(final DiscoverResult searchResult, final SearchResultsRest resultsRest) {
        for (BrowsableDSpaceObject dspaceObject : CollectionUtils.emptyIfNull(searchResult.getDspaceObjects())) {
            SearchResultEntryRest resultEntry = new SearchResultEntryRest();

            //Convert the DSpace Object to its REST model
            resultEntry.setRObject(convertDSpaceObject(dspaceObject));

            //Add hit highlighting for this DSO if present
            DiscoverResult.DSpaceObjectHighlightResult highlightedResults = searchResult
                .getHighlightedResults(dspaceObject);
            if (highlightedResults != null && MapUtils.isNotEmpty(highlightedResults.getHighlightResults())) {
                for (Map.Entry<String, List<String>> metadataHighlight : highlightedResults.getHighlightResults()
                                                                                           .entrySet()) {
                    resultEntry.addHitHighlights(metadataHighlight.getKey(), metadataHighlight.getValue());
                }
            }

            resultsRest.addSearchResult(resultEntry);
        }
    }

    private RestAddressableModel convertDSpaceObject(final Object dspaceObject) {
        for (BrowsableDSpaceObjectConverter<Object, RestAddressableModel> converter : converters) {
            if (converter.supportsModel(dspaceObject)) {
                return converter.convert(dspaceObject);
            }
        }
        return null;
    }

    private void setRequestInformation(final Context context, final String query, final String dsoType,
                                       final String configuration, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page,
                                       final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfiguration(configuration);
        resultsRest.setDsoType(dsoType);

        resultsRest.setScope(scope);

        if (page != null && page.getSort() != null && page.getSort().iterator().hasNext()) {
            Sort.Order order = page.getSort().iterator().next();
            resultsRest.setSort(order.getProperty(), order.getDirection().name());
        }
        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter =
            new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest
                .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
