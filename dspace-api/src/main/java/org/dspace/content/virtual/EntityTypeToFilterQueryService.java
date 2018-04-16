/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.Map;

public class EntityTypeToFilterQueryService {

    private Map<String, String> map;

    public void setMap(Map map) {
        this.map = map;
    }

    public Map getMap() {
        return map;
    }

    public String getFilterQueryForKey(String key) {
        return map.get(key);
    }

    public boolean hasKey(String key) {
        return map.containsKey(key);
    }
}
