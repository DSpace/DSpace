/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a container class in which the variables can be stored that a {@link Process} must adhere to when being
 * retrieved from the DB through the search methods
 */
public class ProcessQueryParameterContainer {


    private Map<String, Object> queryParameterMap = new HashMap<>();

    /**
     * Generic getter for the queryParameterMap
     * @return the queryParameterMap value of this ProcessQueryParameterContainer
     */
    public Map<String, Object> getQueryParameterMap() {
        return queryParameterMap;
    }

    private String sortProperty = "startTime";
    private String sortOrder = "desc";
    /**
     * Generic setter for the queryParameterMap
     * @param queryParameterMap   The queryParameterMap to be set on this ProcessQueryParameterContainer
     */
    public void setQueryParameterMap(Map<String, Object> queryParameterMap) {
        this.queryParameterMap = queryParameterMap;
    }

    public void addToQueryParameterMap(String key, Object object) {
        if (queryParameterMap == null) {
            queryParameterMap = new HashMap<>();
        }
        queryParameterMap.put(key, object);
    }

    /**
     * Generic getter for the sortProperty
     * @return the sortProperty value of this ProcessQueryParameterContainer
     */
    public String getSortProperty() {
        return sortProperty;
    }

    /**
     * Generic setter for the sortProperty
     * @param sortProperty   The sortProperty to be set on this ProcessQueryParameterContainer
     */
    public void setSortProperty(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    /**
     * Generic getter for the sortOrder
     * @return the sortOrder value of this ProcessQueryParameterContainer
     */
    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * Generic setter for the sortOrder
     * @param sortOrder   The sortOrder to be set on this ProcessQueryParameterContainer
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
