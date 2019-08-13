/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.RelationshipType;

/**
 * This class is responsible for holding the representation of how a certain relationshipType label has to be
 * translated to the virtual metadata added onto the items that belong to the relationships that these
 * relationshipTypes belong to
 */
public class VirtualMetadataPopulator {

    /**
     * The map that holds this representation
     */
    private Map<String, HashMap<String, VirtualMetadataConfiguration>> map;

    /**
     * Standard setter for the map
     * @param map   The map to be used in the VirtualMetadataPopulator
     */
    public void setMap(Map<String, HashMap<String, VirtualMetadataConfiguration>> map) {
        this.map = map;
    }

    /**
     * Standard getter for the map
     * @return  The map that is used in the VirtualMetadataPopulator
     */
    public Map<String, HashMap<String, VirtualMetadataConfiguration>> getMap() {
        return map;
    }

    /**
     * This method will return a boolean indicating whether the useForPlace is true or false for the given
     * RelationshipType for the left or right type as indicated by the second parameter.
     * @param relationshipType  The relationshipType for which this should be checked
     * @param isLeft            The boolean indicating whether to check the left or the right type
     * @return                  A boolean indicating whether the useForPlace is true or not for the given parameters
     */
    public boolean isUseForPlaceTrueForRelationshipType(RelationshipType relationshipType, boolean isLeft) {
        HashMap<String, VirtualMetadataConfiguration> hashMaps;
        if (isLeft) {
            hashMaps = this.getMap().get(relationshipType.getLeftwardType());
        } else {
            hashMaps = this.getMap().get(relationshipType.getRightwardType());
        }
        if (hashMaps != null) {
            for (Map.Entry<String, VirtualMetadataConfiguration> entry : hashMaps.entrySet()) {
                VirtualMetadataConfiguration virtualBean = entry.getValue();
                boolean useForPlace = virtualBean.getUseForPlace();
                if (useForPlace) {
                    return true;
                }
            }
        }
        return false;
    }
}
