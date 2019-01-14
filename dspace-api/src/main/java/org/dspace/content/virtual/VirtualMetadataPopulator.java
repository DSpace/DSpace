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
 * This class is responsible for holding the representation of how a certain relationshipType label has to be
 * translated to the virtual metadata added onto the items that belong to the relationships that these
 * relationshipTypes belong to
 */
public class VirtualMetadataPopulator {

    /**
     * The map that holds this representation
     */
    private Map map;

    /**
     * Standard setter for the map
     * @param map   The map to be used in the VirtualMetadataPopulator
     */
    public void setMap(Map map) {
        this.map = map;
    }

    /**
     * Standard getter for the map
     * @return  The map that is used in the VirtualMetadataPopulator
     */
    public Map getMap() {
        return map;
    }
}
