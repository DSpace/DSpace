package org.dspace.app.rest.converter;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
@Component
public class DiscoverResultConverter {

    @Autowired
    public List<DSpaceObjectConverter> converters;

    public Page<SearchResultsRest> convert(final DiscoverQuery discoverQuery, final String configurationName, final String scope,
                                           final List<SearchFilter> searchFilters, final Pageable page, final DiscoverResult searchResult) {

        SearchResultsRest resultsRest = new SearchResultsRest();

        setRequestInformation(discoverQuery, configurationName, scope, searchFilters, page, resultsRest);

        addSearchResults(searchResult, resultsRest);

        return null;
    }

    private <T> void addSearchResults(final DiscoverResult searchResult, final SearchResultsRest resultsRest) {
        for (DSpaceObject dspaceObject : CollectionUtils.emptyIfNull(searchResult.getDspaceObjects())) {
            SearchResultEntryRest resultEntry = new SearchResultEntryRest();

            resultEntry.setDspaceObject(convertDSpaceObject(dspaceObject));
            //TODO HIT HIGHLIGHTING

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

    private void setRequestInformation(final DiscoverQuery discoverQuery, final String configurationName, final String scope, final List<SearchFilter> searchFilters, final Pageable page, final SearchResultsRest resultsRest) {
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
            resultsRest.addAppliedFilter(searchFilter.getName(), searchFilter.getOperator(), searchFilter.getValue(), searchFilter.getValue()); //TODO label
        }
    }
}
