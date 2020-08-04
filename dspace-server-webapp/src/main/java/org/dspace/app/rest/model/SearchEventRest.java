/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class acts as a REST representation for a SearchEvent in DSpace
 */
public class SearchEventRest extends BaseObjectRest<UUID> {

    public static final String NAME = "searchevent";
    public static final String CATEGORY = RestAddressableModel.STATISTICS;

    private String query;
    private UUID scope;
    private String configuration;
    private String dsoType;
    private List<SearchResultsRest.AppliedFilter> appliedFilters;
    private SearchResultsRest.Sorting sort;
    private PageRest page;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public UUID getScope() {
        return scope;
    }

    public void setScope(UUID scope) {
        this.scope = scope;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
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

    public PageRest getPage() {
        return page;
    }

    public void setPage(PageRest page) {
        this.page = page;
    }

    public String getDsoType() {
        return dsoType;
    }

    public void setDsoType(String dsoType) {
        this.dsoType = dsoType;
    }
}
