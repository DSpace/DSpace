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

/**
 * This class' purpose is to create a container for the information used in the SearchFacetValueResource
 */
public class SearchFacetValueRest extends RestAddressableModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String label;
    @JsonIgnore
    private String filterValue;
    private long count;
    @JsonIgnore
    private String authorityKey;
    @JsonIgnore
    private String sortValue;
    @JsonIgnore
    private String filterType;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(final String filterValue) {
        this.filterValue = filterValue;
    }

    public long getCount() {
        return count;
    }

    public void setCount(final long count) {
        this.count = count;
    }

    public void setAuthorityKey(final String authorityKey) {
        this.authorityKey = authorityKey;
    }

    public String getAuthorityKey() {
        return authorityKey;
    }

    public void setSortValue(final String sortValue) {
        this.sortValue = sortValue;
    }

    public String getSortValue() {
        return sortValue;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(final String filterType) {

        this.filterType = filterType;
    }
}
