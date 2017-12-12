/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.*;

/**
 * This class represents a query which the discovery backend can use
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 *
 */
public class DiscoverQuery {

    /** Main attributes for the discovery query **/
    private String query;
    private List<String> filterQueries;
    private int DSpaceObjectFilter = -1;
    private List<String> fieldPresentQueries;
    private boolean spellCheck;

    private int start = 0;
    private int maxResults = -1;

    /** Attributes used for sorting of results **/
    public enum SORT_ORDER {
        desc,
        asc
    }
    private String sortField;
    private SORT_ORDER sortOrder;

    /** Attributes required for the faceting of values **/
    private List<DiscoverFacetField> facetFields;
    private List<String> facetQueries;
    private int facetLimit = -1;
    private int facetMinCount = -1;
    private int facetOffset = 0;
    private Map<String, DiscoverHitHighlightingField> hitHighlighting;

    /** Used when you want to search for a specific field value **/
    private List<String> searchFields;

    /** Misc attributes can be implementation dependent **/
    private Map<String, List<String>> properties;

    public DiscoverQuery() {
        //Initialize all our lists
        this.filterQueries = new ArrayList<String>();
        this.fieldPresentQueries = new ArrayList<String>();

        this.facetFields = new ArrayList<DiscoverFacetField>();
        this.facetQueries = new ArrayList<String>();
        this.searchFields = new ArrayList<String>();
        this.hitHighlighting = new HashMap<String, DiscoverHitHighlightingField>();
        //Use a linked hashmap since sometimes insertion order might matter
        this.properties = new LinkedHashMap<String, List<String>>();
    }


    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setSortField(String sortField, SORT_ORDER sortOrder){
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    public String getSortField() {
        return sortField;
    }

    public SORT_ORDER getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the DSpace object filter, must be an DSpace Object type integer
     * can be used to only return objects from a certain DSpace Object type
     * @param DSpaceObjectFilter the DSpace object filer
     */
    public void setDSpaceObjectFilter(int DSpaceObjectFilter) {
        this.DSpaceObjectFilter = DSpaceObjectFilter;
    }

    /**
     * Gets the DSpace object filter
     * can be used to only return objects from a certain DSpace Object type
     * @return the DSpace object filer
     */
    public int getDSpaceObjectFilter() {
        return DSpaceObjectFilter;
    }

    /**
     * The maximum number of results returned by this query
     * @return the number of results
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results by this query
     * @param maxResults the number of results
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * Adds new filter queries
     * @param filterQueries the filter queries to be added
     */
    public void addFilterQueries(String ...filterQueries){
        this.filterQueries.addAll(Arrays.asList(filterQueries));
    }

    /**
     * Returns the filter queries
     * @return the filter queries in a list
     */
    public List<String> getFilterQueries() {
        return filterQueries;
    }

    /**
     * Adds a query that will ensure that a certain field is present in the index
     * @param fieldPresentQueries the queries to be added
     */
    public void addFieldPresentQueries(String ...fieldPresentQueries){
        this.fieldPresentQueries.addAll(Arrays.asList(fieldPresentQueries));
    }

    public List<String> getFieldPresentQueries() {
        return fieldPresentQueries;
    }

    /**
     * Adds a new facet query
     * @param facetQuery the new facet query to be added
     */
    public void addFacetQuery(String facetQuery){
        this.facetQueries.add(facetQuery);
    }

    /**
     * Returns the facet queries
     * @return the facet queries for this query
     */
    public List<String> getFacetQueries() {
        return facetQueries;
    }

    /**
     * Adds a new facet field
     * @param facetField the new facet field to be added
     */
    public void addFacetField(DiscoverFacetField facetField){
        facetFields.add(facetField);
    }

    /**
     * Gets the facets fields configured
     * @return the facet fields for this query
     */
    public List<DiscoverFacetField> getFacetFields() {
        return facetFields;
    }

    /**
     * Gets the minimum number of values that need to be present before a valid facet value has been found
     * @return facetMinCount the minimum number of values to be present for a valid facet
     */
    public int getFacetMinCount() {
        return facetMinCount;
    }

    /**
     * Set the minimum number of values that need to be present before a valid facet value has been found
     * @param facetMinCount the minimum number of values to be present for a valid facet
     */
    public void setFacetMinCount(int facetMinCount) {
        this.facetMinCount = facetMinCount;
    }

    /**
     * Gets the facet field offset
     * @return the facet field offset
     */
    public int getFacetOffset() {
        return facetOffset;
    }

    /**
     * Sets the facet field offset, one facet offset will be used for all the facet fields
     * @param facetOffset an integer representing the offset
     */
    public void setFacetOffset(int facetOffset) {
        this.facetOffset = facetOffset;
    }

    /**
     * Sets the fields which you want Discovery to return in the search results.
     * It is HIGHLY recommended to limit the fields returned, as by default
     * some backends (like Solr) will return everything.
     * @param field field to add to the list of fields returned
     */
    public void addSearchField(String field){
        this.searchFields.add(field);
    }

    /**
     * Get list of fields which Discovery will return in the search results
     * @return List of field names
     */
    public List<String> getSearchFields() {
        return searchFields;
    }

    /**
     * Returns the misc search properties
     * @return a map containing the properties
     */
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    /**
     * Adds a new search property to the misc search properties
     * @param property the name of the property
     * @param value the value of the property
     */
    public void addProperty(String property, String value){
        List<String> toAddList = properties.get(property);
        if(toAddList == null)
        {
            toAddList = new ArrayList<String>();
        }

        toAddList.add(value);

        properties.put(property, toAddList);
    }

    public DiscoverHitHighlightingField getHitHighlightingField(String field)
    {
        return hitHighlighting.get(field);
    }

    public List<DiscoverHitHighlightingField> getHitHighlightingFields()
    {
        return new ArrayList<DiscoverHitHighlightingField>(hitHighlighting.values());
    }

    public void addHitHighlightingField(DiscoverHitHighlightingField hitHighlighting)
    {
        this.hitHighlighting.put(hitHighlighting.getField(), hitHighlighting);
    }

    public boolean isSpellCheck() {
        return spellCheck;
    }

    public void setSpellCheck(boolean spellCheck) {
        this.spellCheck = spellCheck;
    }
}
