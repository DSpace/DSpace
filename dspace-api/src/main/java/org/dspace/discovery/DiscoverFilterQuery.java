/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

/**
 * This class represents a filter query for discovery and can contain the following objects:
 * The field in which we are searching
 * The query the query which the filter query is using
 * The displayed value
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 *
 */
public class DiscoverFilterQuery {

    private String field;
    private String filterQuery;
    private String displayedValue;


    public DiscoverFilterQuery() {
    }

    public DiscoverFilterQuery(String field, String filterQuery, String displayedValue) {
        this.field = field;
        this.filterQuery = filterQuery;
        this.displayedValue = displayedValue;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public void setDisplayedValue(String displayedValue) {
        this.displayedValue = displayedValue;
    }

    public String getField() {
        return field;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public String getDisplayedValue() {
        return displayedValue;
    }
}
