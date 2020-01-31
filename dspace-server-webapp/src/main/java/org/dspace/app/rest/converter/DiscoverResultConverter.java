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
import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.query.SearchQueryConverter;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
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

    private static final Logger log = Logger.getLogger(DiscoverResultConverter.class);

    @Autowired
    private List<IndexableObjectConverter> converters;
    @Autowired
    private DiscoverFacetsConverter facetConverter;
    @Autowired
    private SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter;

    public SearchResultsRest convert(final Context context, final String query, final String dsoType,
                                     final String configurationName, final String scope,
                                     final List<SearchFilter> searchFilters, final Pageable page,
                                     final DiscoverResult searchResult, final DiscoveryConfiguration configuration,
                                     final Projection projection) {

        SearchResultsRest resultsRest = new SearchResultsRest();
        resultsRest.setProjection(projection);

        setRequestInformation(context, query, dsoType, configurationName, scope, searchFilters, page, resultsRest);

        addSearchResults(searchResult, resultsRest, projection);

        addFacetValues(context, searchResult, resultsRest, configuration, projection);

        resultsRest.setTotalNumberOfResults(searchResult.getTotalSearchResults());

        return resultsRest;
    }

    private void addFacetValues(Context context, final DiscoverResult searchResult, final SearchResultsRest resultsRest,
            final DiscoveryConfiguration configuration, final Projection projection) {
        facetConverter.addFacetValues(context, searchResult, resultsRest, configuration, projection);
    }

    private void addSearchResults(final DiscoverResult searchResult, final SearchResultsRest resultsRest,
                                  final Projection projection) {
        for (IndexableObject dspaceObject : CollectionUtils.emptyIfNull(searchResult.getIndexableObjects())) {
            SearchResultEntryRest resultEntry = new SearchResultEntryRest();
            resultEntry.setProjection(projection);

            //Convert the DSpace Object to its REST model
            resultEntry.setIndexableObject(convertDSpaceObject(dspaceObject, projection));

            //Add hit highlighting for this DSO if present
            DiscoverResult.IndexableObjectHighlightResult highlightedResults = searchResult
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

    private RestAddressableModel convertDSpaceObject(final IndexableObject indexableObject,
                                                     final Projection projection) {
        for (IndexableObjectConverter<Object, RestAddressableModel> converter : converters) {
            if (converter.supportsModel(indexableObject)) {
                return converter.convert(indexableObject.getIndexedObject(), projection);
            }
        }
        return null;
    }

    private void setRequestInformation(final Context context, final String query, final String dsoType,
                                       final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page,
                                       final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfiguration(configurationName);
        resultsRest.setDsoType(dsoType);

        resultsRest.setScope(scope);

        if (page != null && page.getSort() != null && page.getSort().iterator().hasNext()) {
            Sort.Order order = page.getSort().iterator().next();
            resultsRest.setSort(order.getProperty(), order.getDirection().name());
        }
        SearchQueryConverter searchQueryConverter = new SearchQueryConverter();
        List<SearchFilter> transformedFilters = searchQueryConverter.convert(searchFilters);

        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(transformedFilters)) {
            resultsRest
                    .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
