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

/**
 * This class' purpose is to create a container for the information used in the SearchFacetEntryResource
 */
public class SearchFacetEntryRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String name;
    private String facetType;
    private Boolean hasMore = null;

    @JsonIgnore
    private List<SearchFacetValueRest> values;

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
        if(values == null) {
            values = new LinkedList<>();
        }

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Boolean isHasMore() {
        return hasMore;
    }
}
