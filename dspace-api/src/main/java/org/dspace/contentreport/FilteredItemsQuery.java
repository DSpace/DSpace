/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Structured query contents for the Filtered Items report
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemsQuery {

    private List<String> collections = new ArrayList<>();
    private List<QueryPredicate> queryPredicates = new ArrayList<>();
    private long offset;
    private int pageLimit;
    private Set<Filter> filters = EnumSet.noneOf(Filter.class);
    private List<String> additionalFields = new ArrayList<>();

    /**
     * Shortcut method that builds a FilteredItemsQuery instance
     * from its building blocks.
     * @param collectionUuids collection UUIDs to add
     * @param predicates query predicates used to filter existing items
     * @param pageLimit number of items per page
     * @param filters filters to apply to existing items
     * The filters mapping to true will be applied, others (either missing or
     * mapping to false) will not.
     * @param additionalFields additional fields to display in the resulting report
     * @return a FilteredItemsQuery instance built from the provided parameters
     */
    public static FilteredItemsQuery of(Collection<String> collectionUuids,
            Collection<QueryPredicate> predicates, long offset, int pageLimit,
            Collection<Filter> filters, Collection<String> additionalFields) {
        var query = new FilteredItemsQuery();
        Optional.ofNullable(collectionUuids).ifPresent(query.collections::addAll);
        Optional.ofNullable(predicates).ifPresent(query.queryPredicates::addAll);
        query.offset = offset;
        query.pageLimit = pageLimit;
        Optional.ofNullable(filters).ifPresent(query.filters::addAll);
        Optional.ofNullable(additionalFields).ifPresent(query.additionalFields::addAll);
        return query;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections.clear();
        if (collections != null) {
            this.collections.addAll(collections);
        }
    }

    public List<QueryPredicate> getQueryPredicates() {
        return queryPredicates;
    }

    public void setQueryPredicates(List<QueryPredicate> queryPredicates) {
        this.queryPredicates.clear();
        if (queryPredicates != null) {
            this.queryPredicates.addAll(queryPredicates);
        }
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
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
        this.filters.clear();
        if (filters != null) {
            this.filters.addAll(filters);
        }
    }

    public List<String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(List<String> additionalFields) {
        this.additionalFields.clear();
        if (additionalFields != null) {
            this.additionalFields.addAll(additionalFields);
        }
    }

}
