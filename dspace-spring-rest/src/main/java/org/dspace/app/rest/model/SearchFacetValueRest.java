package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetValueRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String label;
    private String filterValue;
    private long count;
    private String authorityKey;
    private String sortValue;
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
