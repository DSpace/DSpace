/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * This class represents a query which the discovery back-end can use.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverQuery {

    /**
     * Main attributes for the discovery query
     **/
    private String query;
    private final List<String> filterQueries;
    private List<String> dspaceObjectFilters = new ArrayList<>();
    private final List<String> fieldPresentQueries;
    private boolean spellCheck;

    private int start = 0;
    private int maxResults = -1;

    /**
     * Attributes used for sorting of results
     **/
    public enum SORT_ORDER {
        desc,
        asc
    }

    private String sortField;
    private SORT_ORDER sortOrder;

    /**
     * Attributes required for the faceting of values
     **/
    private final List<DiscoverFacetField> facetFields;
    private final List<String> facetQueries;
    private int facetMinCount = -1;
    private int facetOffset = 0;
    private final Map<String, DiscoverHitHighlightingField> hitHighlighting;

    /**
     * Used when you want to search for a specific field value
     **/
    private final List<String> searchFields;

    /**
     * Misc attributes can be implementation dependent
     **/
    private final Map<String, List<String>> properties;

    private String discoveryConfigurationName;

    public DiscoverQuery() {
        //Initialize all our lists
        this.filterQueries = new ArrayList<>();
        this.fieldPresentQueries = new ArrayList<>();

        this.facetFields = new ArrayList<>();
        this.facetQueries = new ArrayList<>();
        this.searchFields = new ArrayList<>();
        this.hitHighlighting = new HashMap<>();
        //Use a linked hashmap since sometimes insertion order might matter
        this.properties = new LinkedHashMap<>();
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

    public void setSortField(String sortField, SORT_ORDER sortOrder) {
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
     *
     * @param dspaceObjectFilter the DSpace object filter
     */
    public void setDSpaceObjectFilter(String dspaceObjectFilter) {
        this.dspaceObjectFilters = singletonList(dspaceObjectFilter);
    }

    /**
     * Adds a DSpace object filter, must be an DSpace Object type integer.
     * Can be used to also return objects from a certain DSpace Object type.
     *
     * @param dspaceObjectFilter the DSpace object filer
     */
    public void addDSpaceObjectFilter(String dspaceObjectFilter) {

        if (isNotBlank(dspaceObjectFilter)) {
            this.dspaceObjectFilters.add(dspaceObjectFilter);
        }
    }

    /**
     * Gets the DSpace object filters
     * can be used to only return objects from certain DSpace Object types
     *
     * @return the DSpace object filters
     */
    public List<String> getDSpaceObjectFilters() {
        return dspaceObjectFilters;
    }

    /**
     * The maximum number of results returned by this query
     *
     * @return the number of results
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results by this query
     *
     * @param maxResults the number of results
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * Adds new filter queries
     *
     * @param filterQueries the filter queries to be added
     */
    public void addFilterQueries(String... filterQueries) {
        this.filterQueries.addAll(Arrays.asList(filterQueries));
    }

    /**
     * Returns the filter queries
     *
     * @return the filter queries in a list
     */
    public List<String> getFilterQueries() {
        return filterQueries;
    }

    /**
     * Adds a query that will ensure that a certain field is present in the index
     *
     * @param fieldPresentQueries the queries to be added
     */
    public void addFieldPresentQueries(String... fieldPresentQueries) {
        this.fieldPresentQueries.addAll(Arrays.asList(fieldPresentQueries));
    }

    public List<String> getFieldPresentQueries() {
        return fieldPresentQueries;
    }

    /**
     * Adds a new facet query
     *
     * @param facetQuery the new facet query to be added
     */
    public void addFacetQuery(String facetQuery) {
        this.facetQueries.add(facetQuery);
    }

    /**
     * Returns the facet queries
     *
     * @return the facet queries for this query
     */
    public List<String> getFacetQueries() {
        return facetQueries;
    }

    /**
     * Adds a new facet field
     *
     * @param facetField the new facet field to be added
     */
    public void addFacetField(DiscoverFacetField facetField) {
        facetFields.add(facetField);
    }

    /**
     * Gets the facets fields configured
     *
     * @return the facet fields for this query
     */
    public List<DiscoverFacetField> getFacetFields() {
        return facetFields;
    }

    /**
     * Gets the minimum number of values that need to be present before a valid facet value has been found
     *
     * @return facetMinCount the minimum number of values to be present for a valid facet
     */
    public int getFacetMinCount() {
        return facetMinCount;
    }

    /**
     * Set the minimum number of values that need to be present before a valid facet value has been found
     *
     * @param facetMinCount the minimum number of values to be present for a valid facet
     */
    public void setFacetMinCount(int facetMinCount) {
        this.facetMinCount = facetMinCount;
    }

    /**
     * Gets the facet field offset
     *
     * @return the facet field offset
     */
    public int getFacetOffset() {
        return facetOffset;
    }

    /**
     * Sets the facet field offset, one facet offset will be used for all the facet fields
     *
     * @param facetOffset an integer representing the offset
     */
    public void setFacetOffset(int facetOffset) {
        this.facetOffset = facetOffset;
    }

    /**
     * Sets the fields which you want Discovery to return in the search results.
     * It is HIGHLY recommended to limit the fields returned, as by default
     * some back-ends (like Solr) will return everything.
     *
     * @param field field to add to the list of fields returned
     */
    public void addSearchField(String field) {
        this.searchFields.add(field);
    }

    /**
     * Get list of fields which Discovery will return in the search results
     *
     * @return List of field names
     */
    public List<String> getSearchFields() {
        return searchFields;
    }

    /**
     * Returns the misc search properties
     *
     * @return a map containing the properties
     */
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    /**
     * Adds a new search property to the misc search properties
     *
     * @param property the name of the property
     * @param value    the value of the property
     */
    public void addProperty(String property, String value) {
        List<String> toAddList = properties.get(property);
        if (toAddList == null) {
            toAddList = new ArrayList<>();
        }

        toAddList.add(value);

        properties.put(property, toAddList);
    }

    public DiscoverHitHighlightingField getHitHighlightingField(String field) {
        return hitHighlighting.get(field);
    }

    public List<DiscoverHitHighlightingField> getHitHighlightingFields() {
        return new ArrayList<>(hitHighlighting.values());
    }

    public void addHitHighlightingField(DiscoverHitHighlightingField hitHighlighting) {
        this.hitHighlighting.put(hitHighlighting.getField(), hitHighlighting);
    }

    public boolean isSpellCheck() {
        return spellCheck;
    }

    public void setSpellCheck(boolean spellCheck) {
        this.spellCheck = spellCheck;
    }

    public void addYearRangeFacet(DiscoverySearchFilterFacet facet, FacetYearRange facetYearRange) {
        if (facetYearRange.isValid()) {

            int newestYear = facetYearRange.getNewestYear();
            int oldestYear = facetYearRange.getOldestYear();
            String dateFacet = facetYearRange.getDateFacet();
            int gap = facetYearRange.getYearGap();

            // We need to determine our top year so we can start our count from a clean year
            // Example: 2001 and a gap from 10 we need the following result: 2010 - 2000 ; 2000 - 1990 hence the top
            // year
            int topYear = getTopYear(newestYear, gap);

            if (gap == 1) {
                //We need a list of our years
                //We have a date range add faceting for our field
                //The faceting will automatically be limited to the 10 years in our span due to our filterquery
                this.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), 10,
                                                          facet.getSortOrderSidebar()));
            } else {
                List<String> facetQueries = buildFacetQueriesWithGap(newestYear, oldestYear, dateFacet, gap, topYear,
                                                                     facet.getFacetLimit());
                for (String facetQuery : CollectionUtils.emptyIfNull(facetQueries)) {
                    this.addFacetQuery(facetQuery);
                }
            }
        }
    }

    private List<String> buildFacetQueriesWithGap(int newestYear, int oldestYear, String dateFacet, int gap,
                                                  int topYear, int facetLimit) {
        List<String> facetQueries = new ArrayList<>();
        for (int year = topYear; year > oldestYear && (facetQueries.size() < facetLimit); year -= gap) {
            //Add a filter to remove the last year only if we aren't the last year
            int bottomYear = year - gap;
            //Make sure we don't go below our last year found
            if (bottomYear < oldestYear) {
                bottomYear = oldestYear;
            }

            //Also make sure we don't go above our newest year
            int currentTop = year;
            if ((year == topYear)) {
                currentTop = newestYear;
            } else {
                //We need to do -1 on this one to get a better result
                currentTop--;
            }
            facetQueries.add(dateFacet + ":[" + bottomYear + " TO " + currentTop + "]");
        }
        Collections.reverse(facetQueries);
        return facetQueries;
    }

    private int getTopYear(int newestYear, int gap) {
        return (int) (Math.ceil((float) newestYear / gap) * gap);
    }

    /**
     * Return the name of discovery configuration used by this query
     * 
     * @return the discovery configuration name used
     */
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    /**
     * Set the name of discovery configuration to use to run this query
     * 
     * @param discoveryConfigurationName
     *            the name of the discovery configuration to use to run this query
     */
    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }
}
