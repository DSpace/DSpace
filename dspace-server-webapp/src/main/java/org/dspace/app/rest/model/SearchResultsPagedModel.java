/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.springframework.hateoas.PagedModel;

/**
 * An extended PagedModel exposing search-results information
 */
public class SearchResultsPagedModel extends ExtendedPagedModel<SearchResultEntryRest> {
    private String scope;
    private String query;
    private List<SearchResultsRest.AppliedFilter> appliedFilters;
    private SearchResultsRest.Sorting sort;
    private String configuration;

    public SearchResultsPagedModel(PagedModel<SearchResultEntryRest> pagedModel) {
        super(pagedModel);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchResultsRest.AppliedFilter> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(List<SearchResultsRest.AppliedFilter> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }

    public SearchResultsRest.Sorting getSort() {
        return sort;
    }

    public void setSort(SearchResultsRest.Sorting sort) {
        this.sort = sort;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getType() {
        return SearchResultsRest.NAME;
    }
}
