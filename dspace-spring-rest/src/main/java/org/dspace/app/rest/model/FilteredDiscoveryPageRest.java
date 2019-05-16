/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class acts as the REST representation of the converted EntityType objects to this logic. This class acts
 * as a data holder for the FilteredDiscoveryPageResource
 */
public class FilteredDiscoveryPageRest extends BaseObjectRest<String> {

    public static final String NAME = "filtered-discovery-page";
    public static final String CATEGORY = "config";

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getType() {
        return NAME;
    }

    /**
     * The label of the filter
     */
    @JsonProperty(value = "filter-name")
    private String label;
    /**
     * The filterQuery string that can be used to filter on the label
     */
    @JsonProperty(value = "discovery-query")
    private String filterQueryString;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFilterQueryString(String filterQueryString) {
        this.filterQueryString = filterQueryString; }

    public String getFilterQueryString() {
        return this.filterQueryString; }
}
