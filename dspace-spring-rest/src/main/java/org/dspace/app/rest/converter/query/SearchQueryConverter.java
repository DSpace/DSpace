package org.dspace.app.rest.converter.query;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.query.RestSearchOperator;
import org.dspace.app.rest.parameter.SearchFilter;
import org.apache.commons.collections4.CollectionUtils;
import java.util.LinkedList;
import java.util.List;

public class SearchQueryConverter {

    public List<SearchFilter> convert(List<SearchFilter> searchFilters){

        List<SearchFilter> transformedSearchFilters = new LinkedList<>();
        for(SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)){
            if(StringUtils.equals(searchFilter.getOperator(), "query")){
                SearchFilter transformedSearchFilter = convertQuerySearchFilterIntoStandardSearchFilter(searchFilter);
                transformedSearchFilters.add(transformedSearchFilter);
            } else {
                transformedSearchFilters.add(searchFilter);
            }
        }
        return transformedSearchFilters;

    }

    public SearchFilter convertQuerySearchFilterIntoStandardSearchFilter(SearchFilter searchFilter){
        RestSearchOperator restSearchOperator = RestSearchOperator.forQuery(searchFilter.getValue());
        SearchFilter transformedSearchFilter = new SearchFilter(searchFilter.getName(), restSearchOperator.getDspaceOperator(), restSearchOperator.extractValue(searchFilter.getValue()));
        return transformedSearchFilter;
    }
}
