/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * This class represents the result that the discovery search impl returns
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverResult {

    private long totalSearchResults;
    private int start;
    private List<BrowsableDSpaceObject> dspaceObjects;
    private Map<String, List<FacetResult>> facetResults;
    private Map<String, List<FacetResult>> facetQueryResults;
    private Map<String, List<FacetResult>> facetFieldResults;

    /**
     * A map that contains all the documents sougth after, the key is a string representation of the DSpace object
     */
    private Map<String, List<SearchDocument>> searchDocuments;
    private int maxResults = -1;
    private int searchTime;
    private Map<String, DSpaceObjectHighlightResult> highlightedResults;
    private String spellCheckQuery;

    public DiscoverResult() {
        dspaceObjects = new ArrayList<BrowsableDSpaceObject>();
        facetResults = new LinkedHashMap<String, List<FacetResult>>();
        facetQueryResults = new LinkedHashMap<String, List<FacetResult>>();
        facetFieldResults = new LinkedHashMap<String, List<FacetResult>>();
        searchDocuments = new LinkedHashMap<String, List<SearchDocument>>();
        highlightedResults = new HashMap<String, DSpaceObjectHighlightResult>();
    }

    public void addDSpaceObject(BrowsableDSpaceObject dso) {
        this.dspaceObjects.add(dso);
    }

    public List<BrowsableDSpaceObject> getDspaceObjects() {
        return dspaceObjects;
    }

    public long getTotalSearchResults() {
        return totalSearchResults;
    }

    public void setTotalSearchResults(long totalSearchResults) {
        this.totalSearchResults = totalSearchResults;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(int searchTime) {
        this.searchTime = searchTime;
    }

    public void addFacetResult(String facetField, FacetResult... facetResults) {
        List<FacetResult> facetValues = this.facetResults.get(facetField);
        if (facetValues == null) {
            facetValues = new ArrayList<FacetResult>();
        }
        facetValues.addAll(Arrays.asList(facetResults));
        this.facetResults.put(facetField, facetValues);
    }

    public Map<String, List<FacetResult>> getFacetResults() {
        return facetResults;
    }

    public List<FacetResult> getFacetQueryResult(String facet) {
        return facetQueryResults.get(facet) == null ? new ArrayList<FacetResult>() : facetQueryResults.get(facet);
    }

    public void addFacetQueryResult(String facetField, FacetResult... facetResults) {
        List<FacetResult> facetValues = this.facetQueryResults.get(facetField);
        if (facetValues == null) {
            facetValues = new ArrayList<FacetResult>();
        }
        facetValues.addAll(Arrays.asList(facetResults));
        this.facetQueryResults.put(facetField, facetValues);
    }

    public Map<String, List<FacetResult>> getFacetQueryResults() {
        return facetQueryResults;
    }

    public List<FacetResult> getFacetFieldResult(String facet) {
        return facetFieldResults.get(facet) == null ? new ArrayList<FacetResult>() : facetFieldResults.get(facet);
    }

    public void addFacetFieldResult(String facetField, FacetResult... facetResults) {
        List<FacetResult> facetValues = this.facetFieldResults.get(facetField);
        if (facetValues == null) {
            facetValues = new ArrayList<FacetResult>();
        }
        facetValues.addAll(Arrays.asList(facetResults));
        this.facetFieldResults.put(facetField, facetValues);
    }

    public Map<String, List<FacetResult>> getFacetFieldResults() {
        return facetFieldResults;
    }

    public List<FacetResult> getFacetResult(String facet) {
        return ListUtils.emptyIfNull(facetResults.get(facet));
    }

    public List<FacetResult> getFacetResult(DiscoverySearchFilterFacet field) {
        List<DiscoverResult.FacetResult> facetValues = getFacetResult(field.getIndexFieldName());
        // Check if we are dealing with a date, sometimes the facet values arrive as dates !
        if (facetValues.size() == 0 && field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            facetValues = getFacetResult(field.getIndexFieldName() + ".year");
        }
        return ListUtils.emptyIfNull(facetValues);
    }

    public DSpaceObjectHighlightResult getHighlightedResults(BrowsableDSpaceObject dso) {
        return highlightedResults.get(dso.getUniqueIndexID());
    }

    public void addHighlightedResult(BrowsableDSpaceObject dso, DSpaceObjectHighlightResult highlightedResult) {
        this.highlightedResults.put(dso.getUniqueIndexID(), highlightedResult);
    }

    public static final class FacetResult {
        private String asFilterQuery;
        private String displayedValue;
        private String authorityKey;
        private String sortValue;
        private long count;
        private String fieldType;

        public FacetResult(String asFilterQuery, String displayedValue, String authorityKey, String sortValue,
                long count, String fieldType) {
            this.asFilterQuery = asFilterQuery;
            this.displayedValue = displayedValue;
            this.authorityKey = authorityKey;
            this.sortValue = sortValue;
            this.count = count;
            this.fieldType = fieldType;
        }

        public String getAsFilterQuery() {
            if (asFilterQuery == null) {
                // missing facet filter query
                return "[* TO *]";
            }
            return asFilterQuery;
        }

        public String getDisplayedValue() {
            return displayedValue;
        }

        public String getSortValue() {
            return sortValue;
        }

        public long getCount() {
            return count;
        }

        public String getAuthorityKey() {
            return authorityKey;
        }

        public String getFilterType() {
            return authorityKey != null ? "authority" : asFilterQuery != null ? "equals" : "notequals";
        }

        public String getFieldType() {
            return fieldType;
        }
    }

    public String getSpellCheckQuery() {
        return spellCheckQuery;
    }

    public void setSpellCheckQuery(String spellCheckQuery) {
        this.spellCheckQuery = spellCheckQuery;
    }

    public static final class DSpaceObjectHighlightResult {
        private BrowsableDSpaceObject dso;
        private Map<String, List<String>> highlightResults;
        private Map<String, List<String[]>> highlightResultsWithAuthority;

        public DSpaceObjectHighlightResult(BrowsableDSpaceObject dso, Map<String, List<String>> highlightResults,
                Map<String, List<String[]>> highlightResultsWithAuthority) {
            this.dso = dso;
            this.highlightResults = highlightResults;
            this.highlightResultsWithAuthority = highlightResultsWithAuthority;
        }

        public BrowsableDSpaceObject getDso() {
            return dso;
        }

        public List<String> getHighlightResults(String metadataKey) {
            return highlightResults.get(metadataKey);
        }

        public List<String[]> getHighlightResultsWithAuthority(String metadataKey) {
            return highlightResultsWithAuthority.get(metadataKey);
        }

        public Map<String, List<String>> getHighlightResults() {
            return highlightResults;
        }
    }

    public void addSearchDocument(BrowsableDSpaceObject dso, SearchDocument searchDocument) {
        String dsoString = SearchDocument.getDspaceObjectStringRepresentation(dso);
        List<SearchDocument> docs = searchDocuments.get(dsoString);
        if (docs == null) {
            docs = new ArrayList<SearchDocument>();
        }
        docs.add(searchDocument);
        searchDocuments.put(dsoString, docs);
    }

    /**
     * Returns all the sought after search document values
     *
     * @param dso
     *            the dspace object we want our search documents for
     * @return the search documents list
     */
    public List<SearchDocument> getSearchDocument(BrowsableDSpaceObject dso) {
        String dsoString = SearchDocument.getDspaceObjectStringRepresentation(dso);
        List<SearchDocument> result = searchDocuments.get(dsoString);
        if (result == null) {
            return new ArrayList<SearchDocument>();
        } else {
            return result;
        }
    }

    /**
     * This class contains values from the fields searched for in DiscoveryQuery.java
     */
    public static final class SearchDocument {
        private Map<String, List<String>> searchFields;

        public SearchDocument() {
            this.searchFields = new LinkedHashMap<String, List<String>>();
        }

        public void addSearchField(String field, String... values) {
            List<String> searchFieldValues = searchFields.get(field);
            if (searchFieldValues == null) {
                searchFieldValues = new ArrayList<String>();
            }
            searchFieldValues.addAll(Arrays.asList(values));
            searchFields.put(field, searchFieldValues);
        }

        public Map<String, List<String>> getSearchFields() {
            return searchFields;
        }

        public List<String> getSearchFieldValues(String field) {
            if (searchFields.get(field) == null) {
                return new ArrayList<String>();
            } else {
                return searchFields.get(field);
            }
        }

        public static String getDspaceObjectStringRepresentation(BrowsableDSpaceObject dso) {
            return dso.getType() + ":" + dso.getID();
        }
    }
}
