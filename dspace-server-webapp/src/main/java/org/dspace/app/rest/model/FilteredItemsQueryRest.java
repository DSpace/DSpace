/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.QueryOperator;

/**
 * REST-based version of structured query contents for the Filtered Items report
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemsQueryRest {

    private List<String> collections = new ArrayList<>();
    private List<FilteredItemsQueryPredicate> queryPredicates = new ArrayList<>();
    private int pageLimit;
    private Set<Filter> filters = EnumSet.noneOf(Filter.class);
    private List<String> additionalFields = new ArrayList<>();

    /**
     * Shortcut method that builds a FilteredItemsQueryRest instance
     * from its building blocks.
     * @param collectionUuids collection UUIDs to add
     * @param predicates query predicates used to filter existing items
     * @param pageLimit number of items per page
     * @param filters filters to apply to existing items
     * The filters mapping to true will be applied, others (either missing or
     * mapping to false) will not.
     * @param additionalFields additional fields to display in the resulting report
     * @return a FilteredItemsQueryRest instance built from the provided parameters
     */
    public static FilteredItemsQueryRest of(Collection<String> collectionUuids,
            Collection<FilteredItemsQueryPredicate> predicates, int pageLimit,
            Collection<Filter> filters, Collection<String> additionalFields) {
        var query = new FilteredItemsQueryRest();
        Optional.ofNullable(collectionUuids).ifPresent(query.collections::addAll);
        Optional.ofNullable(predicates).ifPresent(query.queryPredicates::addAll);
        query.pageLimit = pageLimit;
        Optional.ofNullable(filters).ifPresent(query.filters::addAll);
        Optional.ofNullable(additionalFields).ifPresent(query.additionalFields::addAll);
        return query;
    }

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

    public Set<Filter> getFilters() {
        return filters;
    }

    public void setFilters(Set<Filter> filters) {
        this.filters = filters;
    }

    public List<String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(List<String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public String toQueryString() {
        String colls = collections.stream()
                .map(coll -> "collection=" + coll)
                .collect(Collectors.joining("&"));
        String preds = queryPredicates.stream()
                .map(pred -> "queryPredicates=" + pred)
                .collect(Collectors.joining("&"));
        String pgLimit = "pageLimit=" + pageLimit;
        String fltrs = filters.stream()
                .map(e -> "filters=" + e.getId())
                .collect(Collectors.joining("&"));
        String flds = additionalFields.stream()
                .map(fld -> "additionalFields=" + fld)
                .collect(Collectors.joining("&"));

        return Stream.of(colls, preds, pgLimit, fltrs, flds)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("&"));
    }

}
