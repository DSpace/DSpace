package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
public class SearchResultsRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String query;
    private String dsoType;
    private String scope;
    private String configurationName;
    private List<AppliedFilter> appliedFilters;
    private Sorting sort;

    @JsonIgnore
    List<SearchResultEntryRest> searchResults;

    //TODO List<SearchFacetEntryRest> facets;

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

    public List<AppliedFilter> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(final List<AppliedFilter> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }

    public void addAppliedFilter(final String name, final String operator, final String value, final String label) {
        if(appliedFilters == null) {
            appliedFilters = new LinkedList<>();
        }

        appliedFilters.add(new AppliedFilter(name, operator, value, label));
    }

    public Sorting getSort() {
        return sort;
    }

    public void setSort(final Sorting sort) {
        this.sort = sort;
    }

    public void setSort(final String property, final String direction) {
        sort = new Sorting(property, direction);
    }

    public List<SearchResultEntryRest> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(final List<SearchResultEntryRest> searchResults) {
        this.searchResults = searchResults;
    }

    public void addSearchResult(SearchResultEntryRest searchResultEntry) {
        if(searchResults == null) {
            searchResults = new LinkedList<>();
        }

        searchResults.add(searchResultEntry);
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(final String configurationName) {
        this.configurationName = configurationName;
    }

    public class AppliedFilter {

        private String filter;
        private String operator;
        private String value;
        private String label;

        public AppliedFilter(final String name, final String operator, final String value, final String label) {
            this.filter = name;
            this.operator = operator;
            this.value = value;
            this.label = label;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(final String filter) {
            this.filter = filter;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(final String operator) {
            this.operator = operator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }
    }

    private class Sorting {
        private String by;
        private String order;

        public Sorting(String by, String order) {
            this.by = by;
            this.order = order;
        }

        public String getBy() {
            return by;
        }

        public void setBy(final String by) {
            this.by = by;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(final String order) {
            this.order = order;
        }
    }
}
