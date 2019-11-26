/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.parameter.SearchFilter;

/**
 * This class provides a container for the information to be used in the DiscoveryResultsResource
 */
public abstract class DiscoveryResultsRest extends BaseObjectRest<String> {

    @JsonIgnore
    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;
    private String scope;
    private String query;
    private List<SearchResultsRest.AppliedFilter> appliedFilters;
    private SearchResultsRest.Sorting sort;
    @JsonIgnore
    private String dsoType;
    @JsonIgnore
    private List<SearchFilter> searchFilters;
    private String configuration;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getDsoType() {
        return dsoType;
    }

    public void setDsoType(final String dsoType) {
        this.dsoType = dsoType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public List<SearchResultsRest.AppliedFilter> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(final List<SearchResultsRest.AppliedFilter> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }

    public void addAppliedFilter(final SearchResultsRest.AppliedFilter filter) {
        if (appliedFilters == null) {
            appliedFilters = new LinkedList<>();
        }

        appliedFilters.add(filter);
    }

    public SearchResultsRest.Sorting getSort() {
        return sort;
    }

    public void setSort(final SearchResultsRest.Sorting sort) {
        this.sort = sort;
    }

    public void setSort(final String property, final String direction) {
        sort = new SearchResultsRest.Sorting(property, direction);
    }


    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }

    public void setSearchFilters(final List<SearchFilter> searchFilters) {
        this.searchFilters = searchFilters;
    }

    public List<SearchFilter> getSearchFilters() {
        return searchFilters;
    }


}
