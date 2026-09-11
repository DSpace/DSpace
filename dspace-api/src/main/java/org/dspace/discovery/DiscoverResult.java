/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.discovery.SolrServiceImpl.SOLR_FIELD_SUFFIX_FACET_PREFIXES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Strings;
import org.apache.solr.client.solrj.response.PivotField;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.GraphDiscoverSearchFilterFacet;

/**
 * This class represents the result that the discovery search impl returns
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverResult {

    private long totalSearchResults;
    private int start;
    private List<IndexableObject> indexableObjects;
    private Map<String, List<FacetResult>> facetResults;
    private Map<String, Long> facetResultsMissing;
    private Map<String, Long> facetResultMore;
    private Map<String, Long> facetResultTotalElements;
    private Map<String, List<FacetPivotResult>> facetPivotResults;

    // Total count of facet entries calculated for a metadata browsing query
    private long totalEntries;

    /**
     * A map that contains all the documents sougth after, the key is a string representation of the Indexable Object
     */
    private Map<String, List<SearchDocument>> searchDocuments;
    private int maxResults = -1;
    private int searchTime;
    private Map<String, IndexableObjectHighlightResult> highlightedResults;
    private String spellCheckQuery;

    public DiscoverResult() {
        indexableObjects = new ArrayList<IndexableObject>();
        facetResults = new LinkedHashMap<String, List<FacetResult>>();
        facetPivotResults = new LinkedHashMap<String, List<FacetPivotResult>>();
        searchDocuments = new LinkedHashMap<String, List<SearchDocument>>();
        facetResultsMissing = new LinkedHashMap<String, Long>();
        facetResultMore = new LinkedHashMap<String, Long>();
        facetResultTotalElements = new LinkedHashMap<String, Long>();
        highlightedResults = new HashMap<String, IndexableObjectHighlightResult>();
    }

    public void addIndexableObject(IndexableObject idxObj) {
        this.indexableObjects.add(idxObj);
    }

    public List<IndexableObject> getIndexableObjects() {
        return indexableObjects;
    }

    public long getTotalSearchResults() {
        return totalSearchResults;
    }

    public void setTotalSearchResults(long totalSearchResults) {
        this.totalSearchResults = totalSearchResults;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
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

    public void addFacetPivotResult(String facetPivot, FacetPivotResult... pivotResults) {
        List<FacetPivotResult> facetValues = this.facetPivotResults.get(facetPivot);
        if (facetValues == null) {
            facetValues = new ArrayList<FacetPivotResult>();
        }
        facetValues.addAll(Arrays.asList(pivotResults));
        this.facetPivotResults.put(facetPivot, facetValues);
    }

    public void setFacetResultMissing(String facetField, long missingCount) {
        facetResultsMissing.put(facetField, missingCount);
    }

    public Long getFacetResultMissing(String facetField) {
        return facetResultsMissing.get(facetField);
    }

    public void setFacetResultMore(String field, long l) {
        facetResultMore.put(field, l);
    }

    public Long getFacetResultMore(String facetField) {
        return facetResultMore.get(facetField);
    }

    public void setFacetResultTotalElements(String field, Long countDistinct) {
        facetResultTotalElements.put(field, countDistinct);
    }

    public Long getFacetResultTotalElements(String facetField) {
        return facetResultTotalElements.get(facetField);
    }

    public Map<String, List<FacetResult>> getFacetResults() {
        return facetResults;
    }

    public List<FacetResult> getFacetResult(String facet) {
        return ListUtils.emptyIfNull(facetResults.get(facet));
    }

    public Map<String, List<FacetPivotResult>> getFacetPivotResults() {
        return facetPivotResults;
    }

    public List<FacetPivotResult> getFacetPivotResult(String facetPivot) {
        return ListUtils.emptyIfNull(facetPivotResults.get(facetPivot));
    }

    public List<FacetResult> getFacetResult(DiscoverySearchFilterFacet field) {
        String facetName = field.getIndexFieldName();
        if (Strings.CS.startsWith(field.getIndexFieldName(), GraphDiscoverSearchFilterFacet.TYPE_PREFIX)) {
            facetName = facetName.split("\\.", 3)[2];
        }
        List<DiscoverResult.FacetResult> facetValues = getFacetResult(facetName);
        // Check if we are dealing with a date, sometimes the facet values arrive as dates !
        if (facetValues.size() == 0 && field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            facetValues = getFacetResult(field.getIndexFieldName() + ".year");
        }
        if (facetValues.isEmpty()) {
            facetValues = getFacetResult(field.getIndexFieldName() + SOLR_FIELD_SUFFIX_FACET_PREFIXES);
        }
        return ListUtils.emptyIfNull(facetValues);
    }

    public IndexableObjectHighlightResult getHighlightedResults(IndexableObject dso) {
        return highlightedResults.get(dso.getUniqueIndexID());
    }

    public void addHighlightedResult(IndexableObject dso, IndexableObjectHighlightResult highlightedResult) {
        this.highlightedResults.put(dso.getUniqueIndexID(), highlightedResult);
    }

    public static final class FacetResult {
        private String asFilterQuery;
        private String displayedValue;
        private String authorityKey;
        private String sortValue;
        private long count;
        private String fieldType;
        private int missing;
        private int more;
        private int totalElements;

        public FacetResult(String asFilterQuery, String displayedValue, String authorityKey, String sortValue,
                long count, String fieldType) {
            this.asFilterQuery = asFilterQuery;
            this.displayedValue = displayedValue;
            this.authorityKey = authorityKey;
            this.sortValue = sortValue;
            this.count = count;
            this.fieldType = fieldType;
            this.missing = -1;
            this.more = -1;
            this.totalElements = -1;
        }

        public FacetResult(String asFilterQuery, String displayedValue, String authorityKey, String sortValue,
                long count, String fieldType, int missing, int more, int totalElements) {
            this.asFilterQuery = asFilterQuery;
            this.displayedValue = displayedValue;
            this.authorityKey = authorityKey;
            this.sortValue = sortValue;
            this.count = count;
            this.fieldType = fieldType;
            this.missing = missing;
            this.more = more;
            this.totalElements = totalElements;
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

        public int getMissing() {
            return missing;
        }

        public int getMore() {
            return more;
        }

        public int getTotalElements() {
            return totalElements;
        }
    }

    public static class FacetPivotResult {

        private long count;

        private String value;

        private FacetPivotResult[] pivot;

        public FacetPivotResult(long count, String value, FacetPivotResult[] pivot) {
            this.count = count;
            this.value = value;
            this.pivot = pivot;
        }

        public static FacetPivotResult[] fromPivotFields(List<PivotField> pivotFields) {
            return ListUtils.emptyIfNull(pivotFields).stream()
                .map(FacetPivotResult::fromPivotField)
                .toArray(FacetPivotResult[]::new);
        }

        public static FacetPivotResult fromPivotField(PivotField pivotField) {
            int count = pivotField.getCount();
            String value = String.valueOf(pivotField.getValue());
            FacetPivotResult[] pivot = fromPivotFields(pivotField.getPivot());
            return new FacetPivotResult(count, value, pivot);
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public FacetPivotResult[] getPivot() {
            return pivot;
        }

        public void setPivot(FacetPivotResult[] pivot) {
            this.pivot = pivot;
        }

    }

    public String getSpellCheckQuery() {
        return spellCheckQuery;
    }

    public void setSpellCheckQuery(String spellCheckQuery) {
        this.spellCheckQuery = spellCheckQuery;
    }

    /**
     * An utility class to represent the highlighting section of a Discovery Search
     *
     */
    public static final class IndexableObjectHighlightResult {
        private IndexableObject indexableObject;
        private Map<String, List<String>> highlightResults;

        public IndexableObjectHighlightResult(IndexableObject idxObj, Map<String, List<String>> highlightResults) {
            this.indexableObject = idxObj;
            this.highlightResults = highlightResults;
        }

        /**
         * Return the indexable object that the highlighting snippets refer to
         * 
         * @return the indexable object
         */
        public IndexableObject getIndexableObject() {
            return indexableObject;
        }

        /**
         * The matching snippets for a specific metadata ignoring any authority value
         * 
         * @param metadataKey
         *            the metadata where the snippets have been found
         * @return the matching snippets
         */
        public List<String> getHighlightResults(String metadataKey) {
            return highlightResults.get(metadataKey);
        }

        /**
         * All the matching snippets in whatever metadata ignoring any authority value
         * 
         * @return All the matching snippets
         */
        public Map<String, List<String>> getHighlightResults() {
            return highlightResults;
        }
    }

    public void addSearchDocument(IndexableObject dso, SearchDocument searchDocument) {
        String dsoString = SearchDocument.getIndexableObjectStringRepresentation(dso);
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
     * @param idxObj
     *            the indexable object we want our search documents for
     * @return the search documents list
     */
    public List<SearchDocument> getSearchDocument(IndexableObject idxObj) {
        String dsoString = SearchDocument.getIndexableObjectStringRepresentation(idxObj);
        List<SearchDocument> result = searchDocuments.get(dsoString);
        if (result == null) {
            return new ArrayList<>();
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

        public static String getIndexableObjectStringRepresentation(IndexableObject idxObj) {
            return idxObj.getType() + ":" + idxObj.getID();
        }
    }

}
