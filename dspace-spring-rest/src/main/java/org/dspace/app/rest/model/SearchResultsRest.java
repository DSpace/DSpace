/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
public class SearchResultsRest extends ResultsRest {



    private long totalNumberOfResults;

    @JsonIgnore
    List<SearchResultEntryRest> searchResults;

    @JsonIgnore
    List<SearchFacetEntryRest> facets;


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


    public long getTotalNumberOfResults() {
        return totalNumberOfResults;
    }

    public void setTotalNumberOfResults(long totalNumberOfResults) {
        this.totalNumberOfResults = totalNumberOfResults;
    }

    public void addFacetEntry(final SearchFacetEntryRest facetEntry) {
        if(facets == null) {
            facets = new LinkedList<>();
        }

        facets.add(facetEntry);
    }

    public List<SearchFacetEntryRest> getFacets() {
        return facets;
    }

    public static class AppliedFilter {

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

    public static class Sorting {
        private String by;
        private String order;

        public Sorting(String by, String order) {
            this.by = by;
            this.order = order;
        }
        public Sorting(String by){
            this.by=by;
            this.order=null;
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
