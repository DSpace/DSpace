package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetValueRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String value;
    private long count;
    private String authorityKey;
    private String sortValue;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
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
}
