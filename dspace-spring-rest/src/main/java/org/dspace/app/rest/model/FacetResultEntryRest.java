package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedList;
import java.util.List;

public class FacetResultEntryRest extends BaseObjectRest<String> {


    public String getCategory() {
        return null;
    }

    public String getType() {
        return null;
    }

    public Class getController() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    private String name;
    private long count;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<SearchResultsRest.AppliedFilter> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(List<SearchResultsRest.AppliedFilter> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }

    @JsonIgnore
    private String query;
    @JsonIgnore
    private String scope;
    @JsonIgnore
    private List<SearchResultsRest.AppliedFilter> appliedFilters;

    @JsonIgnore
    private String facetName;

    public String getFacetName(){
        return facetName;
    }
    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }
}
