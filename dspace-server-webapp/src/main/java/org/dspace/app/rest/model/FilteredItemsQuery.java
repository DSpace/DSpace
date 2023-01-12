/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.app.rest.contentreport.Filter;
import org.dspace.contentreport.QueryOperator;

/**
 * Structured query contents for the Filtered Items report
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemsQuery {

    private List<String> collections = new ArrayList<>();
    private List<FilteredItemsQueryPredicate> queryPredicates = new ArrayList<>();
    private int pageLimit;
    private Map<Filter, Boolean> filters = new EnumMap<>(Filter.class);
    private List<String> additionalFields = new ArrayList<>();

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public List<FilteredItemsQueryPredicate> getQueryPredicates() {
        return queryPredicates;
    }

    public void setQueryPredicates(List<FilteredItemsQueryPredicate> queryPredicates) {
        this.queryPredicates = queryPredicates;
    }

    public List<String> getPredicateFields() {
        if (queryPredicates == null) {
            return Collections.emptyList();
        }
        return queryPredicates.stream()
                .map(FilteredItemsQueryPredicate::getField)
                .collect(Collectors.toList());
    }

    public List<QueryOperator> getPredicateOperators() {
        if (queryPredicates == null) {
            return Collections.emptyList();
        }
        return queryPredicates.stream()
                .map(FilteredItemsQueryPredicate::getOperator)
                .collect(Collectors.toList());
    }

    public List<String> getPredicateValues() {
        if (queryPredicates == null) {
            return Collections.emptyList();
        }
        return queryPredicates.stream()
                .map(FilteredItemsQueryPredicate::getValue)
                .map(s -> s == null ? "" : s)
                .collect(Collectors.toList());
    }

    public int getPageLimit() {
        return pageLimit;
    }

    public void setPageLimit(int pageLimit) {
        this.pageLimit = pageLimit;
    }

    public Map<Filter, Boolean> getFilters() {
        return filters;
    }

    public void setFilters(Map<Filter, Boolean> filters) {
        this.filters = filters;
    }

    public Set<Filter> getEnabledFilters() {
        return filters.entrySet().stream()
                .filter(e -> e.getValue().booleanValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Filter.class)));
    }

    public List<String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(List<String> additionalFields) {
        this.additionalFields = additionalFields;
    }

}
