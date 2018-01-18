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
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
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
    private List<DSpaceObjectConverter> converters;

    private DiscoverFacetValueConverter facetValueConverter = new DiscoverFacetValueConverter();

    public SearchResultsRest convert(final Context context, final String query, final String dsoType, final String configurationName, final String scope,
                                     final List<SearchFilter> searchFilters, final Pageable page, final DiscoverResult searchResult, final DiscoveryConfiguration configuration) {

        SearchResultsRest resultsRest = new SearchResultsRest();

        setRequestInformation(context, query, dsoType, configurationName, scope, searchFilters, page, resultsRest);

        addSearchResults(searchResult, resultsRest);

        addFacetValues(searchResult, resultsRest, configuration);

        resultsRest.setTotalNumberOfResults(searchResult.getTotalSearchResults());

        return resultsRest;
    }

    private void addFacetValues(final DiscoverResult searchResult, final SearchResultsRest resultsRest, final DiscoveryConfiguration configuration) {

        List<DiscoverySearchFilterFacet> facets = configuration.getSidebarFacets();
        for (DiscoverySearchFilterFacet field : CollectionUtils.emptyIfNull(facets)) {
            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(field.getIndexFieldName());
            int valueCount = 0;
            facetEntry.setHasMore(false);
            facetEntry.setFacetLimit(field.getFacetLimit());

            for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
                //The discover results contains max facetLimit + 1 values. If we reach the "+1", indicate that there are
                //more results available.
                if(valueCount < field.getFacetLimit()) {
                    SearchFacetValueRest valueRest = facetValueConverter.convert(value);

                    facetEntry.addValue(valueRest);
                } else {
                    facetEntry.setHasMore(true);
                }

                if(StringUtils.isBlank(facetEntry.getFacetType())) {
                    facetEntry.setFacetType(value.getFieldType());
                }

                valueCount++;
            }

            resultsRest.addFacetEntry(facetEntry);
        }
    }

    private void addSearchResults(final DiscoverResult searchResult, final SearchResultsRest resultsRest) {
        for (DSpaceObject dspaceObject : CollectionUtils.emptyIfNull(searchResult.getDspaceObjects())) {
            SearchResultEntryRest resultEntry = new SearchResultEntryRest();

            //Convert the DSpace Object to its REST model
            resultEntry.setDspaceObject(convertDSpaceObject(dspaceObject));

            //Add hit highlighting for this DSO if present
            DiscoverResult.DSpaceObjectHighlightResult highlightedResults = searchResult.getHighlightedResults(dspaceObject);
            if(highlightedResults != null && MapUtils.isNotEmpty(highlightedResults.getHighlightResults())) {
                for (Map.Entry<String, List<String>> metadataHighlight : highlightedResults.getHighlightResults().entrySet()) {
                    resultEntry.addHitHighlights(metadataHighlight.getKey(), metadataHighlight.getValue());
                }
            }

            resultsRest.addSearchResult(resultEntry);
        }
    }

    private DSpaceObjectRest convertDSpaceObject(final DSpaceObject dspaceObject) {
        for (DSpaceObjectConverter converter : converters) {
            if(converter.supportsModel(dspaceObject)) {
                return converter.fromModel(dspaceObject);
            }
        }
        return null;
    }

    private void setRequestInformation(final Context context, final String query, final String dsoType, final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page, final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfigurationName(configurationName);
        resultsRest.setDsoType(dsoType);

        resultsRest.setScope(scope);

        if(page != null && page.getSort() != null && page.getSort().iterator().hasNext()) {
            Sort.Order order = page.getSort().iterator().next();
            resultsRest.setSort(order.getProperty(), order.getDirection().name());
        }
        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest.addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
