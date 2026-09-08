/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.query;

import static org.dspace.app.rest.model.SearchConfigurationRest.Filter.OPERATOR_QUERY;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.dspace.app.rest.model.query.RestSearchOperator;
import org.dspace.app.rest.parameter.SearchFilter;

/**
 * Utility class for transforming a list of SearchFilters. Each SearchFilter with an operator set to 'query'
 * is converted into a SearchFilter with a standard DSpace operator like 'contains'.
 */
public class SearchQueryConverter {

    /**
     * This method traverses the list of SearchFilters and transforms all of those that with 'query'
     * as the operator into a standard DSpace SearchFilter
     *
     * @param searchFilters list of SearchFilters to be transformed
     * @return list of transformed SearchFilters
     */
    public List<SearchFilter> convert(List<SearchFilter> searchFilters) {

        List<SearchFilter> transformedSearchFilters = new LinkedList<>();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            if (Strings.CS.equals(searchFilter.getOperator(), OPERATOR_QUERY)) {
                SearchFilter transformedSearchFilter = convertQuerySearchFilterIntoStandardSearchFilter(searchFilter);
                transformedSearchFilters.add(transformedSearchFilter);
            } else {
                transformedSearchFilters.add(searchFilter);
            }
        }
        return transformedSearchFilters;

    }

    /**
     * This method takes care of the converter of a specific SearchFilter given to it
     *
     * @param searchFilter searchFilter to be transformed
     * @return transformed SearchFilter
     */
    private SearchFilter convertQuerySearchFilterIntoStandardSearchFilter(SearchFilter searchFilter) {
        RestSearchOperator restSearchOperator = RestSearchOperator.forQuery(searchFilter.getValue());
        SearchFilter transformedSearchFilter = new SearchFilter(searchFilter.getName(),
                restSearchOperator.getDspaceOperator(), restSearchOperator.extractValue(searchFilter.getValue()));
        return transformedSearchFilter;
    }
}
