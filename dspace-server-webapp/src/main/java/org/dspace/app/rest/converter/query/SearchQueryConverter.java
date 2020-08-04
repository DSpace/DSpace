/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.query.RestSearchOperator;
import org.dspace.app.rest.parameter.SearchFilter;

/**
 * This method will traverse a list of SearchFilters and transform any SearchFilters with an operator
 * this is equal to 'Query' into a SearchFilter that has a standard DSpace operator like 'contains'
 */
public class SearchQueryConverter {

    /**
     * This method traverses the list of SearchFilters and transforms all of those that contain 'Query'
     * as the operator into a standard DSpace SearchFilter
     *
     * @param   searchFilters The list of SearchFilters to be used
     * @return  A list of transformed SearchFilters
     */
    public List<SearchFilter> convert(List<SearchFilter> searchFilters) {

        List<SearchFilter> transformedSearchFilters = new LinkedList<>();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
            if (StringUtils.equals(searchFilter.getOperator(), "query")) {
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
     * @param searchFilter  The SearchFilter to be transformed
     * @return  The transformed SearchFilter
     */
    public SearchFilter convertQuerySearchFilterIntoStandardSearchFilter(SearchFilter searchFilter) {
        RestSearchOperator restSearchOperator = RestSearchOperator.forQuery(searchFilter.getValue());
        SearchFilter transformedSearchFilter = new SearchFilter(searchFilter.getName(),
                restSearchOperator.getDspaceOperator(), restSearchOperator.extractValue(searchFilter.getValue()));
        return transformedSearchFilter;
    }
}
