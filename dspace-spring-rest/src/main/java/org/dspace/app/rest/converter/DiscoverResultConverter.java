package org.dspace.app.rest.converter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * TODO TOM UNIT TEST
 */
@Component
public class DiscoverResultConverter {

    @Autowired
    private List<DSpaceObjectConverter> converters;

    @Autowired
    private AuthorityValueService authorityValueService;


    public SearchResultsRest convert(final Context context, final DiscoverQuery discoverQuery, final String configurationName, final String scope,
                                     final List<SearchFilter> searchFilters, final Pageable page, final DiscoverResult searchResult) {

        SearchResultsRest resultsRest = new SearchResultsRest();

        setRequestInformation(context, discoverQuery, configurationName, scope, searchFilters, page, resultsRest);

        addSearchResults(searchResult, resultsRest);

        resultsRest.setTotalNumberOfResults(searchResult.getTotalSearchResults());

        return resultsRest;
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

    private void setRequestInformation(final Context context, final DiscoverQuery discoverQuery, final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page, final SearchResultsRest resultsRest) {
        resultsRest.setQuery(discoverQuery.getQuery());
        resultsRest.setConfigurationName(configurationName);

        if(discoverQuery.getDSpaceObjectFilter() >= 0) {
            resultsRest.setDsoType(Constants.typeText[discoverQuery.getDSpaceObjectFilter()]);
        }

        resultsRest.setScope(scope);

        if(page != null && page.getSort() != null && page.getSort().iterator().hasNext()) {
            Sort.Order order = page.getSort().iterator().next();
            resultsRest.setSort(order.getProperty(), order.getDirection().name());
        }

        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest.addAppliedFilter(convertSearchFilter(context, searchFilter));
        }
    }

    private SearchResultsRest.AppliedFilter convertSearchFilter(Context context, SearchFilter searchFilter) {
        AuthorityValue authorityValue = null;
        if(searchFilter.hasAuthorityOperator()) {
            authorityValue = authorityValueService.findByUID(context, searchFilter.getValue());
        }

        SearchResultsRest.AppliedFilter appliedFilter;
        if (authorityValue == null) {
            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
                    searchFilter.getValue(), searchFilter.getValue());
        } else {
            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
                    searchFilter.getValue(), authorityValue.getValue());
        }

        return appliedFilter;
    }
}
