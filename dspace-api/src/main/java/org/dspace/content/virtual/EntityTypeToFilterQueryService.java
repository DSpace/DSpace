/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.Map;

/**
 * This service offers a way to convert EntityType String labels to a filter query which is defined in the
 * bean config for this service
 */
public class EntityTypeToFilterQueryService {

    /**
     * This map contains the mapping between the String label and the String for the filter query
     * e.g. <entry key="Person" value="f.entityType=Person,equals"/>
     */
    private Map<String, String> map;


    /**
     * Standard setter for this map
     * @param map   The map that should be set in this service
     */
    public void setMap(Map map) {
        this.map = map;
    }

    /**
     * Standard getter for the map
     * @return  the map
     */
    public Map getMap() {
        return map;
    }

    /**
     * Retrieves the filterQuery for the key that's given as a parameter. It looks in the map for the value
     * @param key   The key for which we'll find the value in the map
     * @return      The filter query representation for the given key
     */
    public String getFilterQueryForKey(String key) {
        return map.get(key);
    }

    /**
     * Returns a boolean depending on whether a key is present in the map or not
     * @param key   The key to be checked for
     * @return      The boolean indicating whether this key is present in the map or not
     */
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }
}
