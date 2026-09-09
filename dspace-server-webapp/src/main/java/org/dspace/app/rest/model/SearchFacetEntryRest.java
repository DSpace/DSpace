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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * This class' purpose is to create a container for the information used in the SearchFacetEntryResource
 */
public class SearchFacetEntryRest extends RestAddressableModel {

    public static final String NAME = "discover";
    public static final String PLURAL_NAME = NAME;
    public static final String CATEGORY = RestModel.DISCOVER;

    private String name;
    private String facetType;

    @JsonIgnore
    private Boolean hasMore = null;
    private int facetLimit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String missing;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String more;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String totalElements;

    @JsonIgnore
    private Boolean isOpenByDefault;

    @JsonIgnore
    private boolean exposeMinMax = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String minValue;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String maxValue;

    @JsonIgnore
    private List<SearchFacetValueRest> values = new LinkedList<>();
    private boolean exposeMore;
    private boolean exposeMissing;

    public SearchFacetEntryRest(final String name) {
        this.name = name;
    }

    public String getCategory() {
        return CATEGORY;
    }

    @JsonIgnore
    public String getType() {
        return NAME;
    }

    /**
     * The plural name is the same as the singular name
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void addValue(final SearchFacetValueRest valueRest) {
        values.add(valueRest);
    }

    public List<SearchFacetValueRest> getValues() {
        return values;
    }

    public String getFacetType() {
        return facetType;
    }

    public void setFacetType(final String facetType) {
        this.facetType = facetType;
    }

    public void setHasMore(final Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Boolean isHasMore() {
        return hasMore;
    }

    public int getFacetLimit() {
        return facetLimit;
    }

    public void setFacetLimit(final int facetLimit) {
        this.facetLimit = facetLimit;
    }

    public void setOpenByDefault(boolean isOpenByDefault) {
        this.isOpenByDefault = Boolean.valueOf(isOpenByDefault);
    }
    /**
     * See documentation at {@link DiscoverySearchFilter#isOpenByDefault()}
     */
    public Boolean isOpenByDefault() {
        return this.isOpenByDefault;
    }

    /**
     * See documentation at {@link DiscoverySearchFilterFacet#exposeMinAndMaxValue()}
     */
    public boolean exposeMinAndMaxValue() {
        return exposeMinMax;
    }
    /**
     * See documentation at {@link DiscoverySearchFilterFacet#setExposeMinAndMaxValue(boolean)}
     */
    public void setExposeMinMax(boolean exposeMinMax) {
        this.exposeMinMax = exposeMinMax;
    }

    /**
     * Returns the smallest value that can be found for this value in solr
     * @return  The smallest value for this facet as a String
     */
    public String getMinValue() {
        return minValue;
    }

    /**
     * Sets the minValue property for the SearchFacetEntryRest to the given parameter
     * @param minValue  The String that the minValue will be set to
     */
    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    /**
     * Returns the highest value that can be found for this value in solr
     * @return  The highest value for this facet as a String
     */
    public String getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maxValue property for the SearchFacetEntryRest to the given parameter
     * @param maxValue  The String that the maxValue will be set to
     */
    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public void setMissing(String missing) {
        this.missing = missing;
    }

    public String getMissing() {
        return missing;
    }

    public void setMore(String more) {
        this.more = more;
    }

    public String getMore() {
        return more;
    }

    public void setTotalElements(String totalElements) {
        this.totalElements = totalElements;
    }

    public String getTotalElements() {
        return totalElements;
    }

    public boolean exposeMore() {
        return exposeMore;
    }

    public void setExposeMore(boolean exposeMore) {
        this.exposeMore = exposeMore;
    }

    public boolean exposeMissing() {
        return exposeMissing;
    }

    public void setExposeMissing(boolean exposeMissing) {
        this.exposeMissing = exposeMissing;
    }
}
