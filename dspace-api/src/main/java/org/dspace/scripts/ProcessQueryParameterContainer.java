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
}
