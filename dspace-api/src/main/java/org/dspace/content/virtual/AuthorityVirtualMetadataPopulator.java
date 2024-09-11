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

/**
 * This class simply sets and gets maps of maps of string-{@link AuthorityVirtualMetadataConfiguration}s, where the key
 * is an authority-controlled metadata field like dc.subject, and the key in the value map is the virtual field name
 * to set / transform for a given set of values (e.g. dc.description, dcterms.spatial).
 * It operates in a similar way to entity {@link VirtualMetadataPopulator}
 *
 * @author Kim Shepherd
 */
public class AuthorityVirtualMetadataPopulator {

    /**
     * The map of authority controlled field names, to a map of virtual metadata field names and configurations.
     */
    private Map<String, HashMap<String, AuthorityVirtualMetadataConfiguration>> map;

    /**
     * Sets the map of authority controlled field names to a map of virtual metadata field names and configurations.
     * The map is used for authority virtual metadata population.
     *
     * @param map the map of authority controlled field names to virtual metadata field names and configurations
     */
    public void setMap(Map<String, HashMap<String, AuthorityVirtualMetadataConfiguration>> map) {
        this.map = map;
    }

    /**
     * Retrieves the map of authority controlled field names to a map of virtual metadata field names and configurations.
     *
     * @return the map of authority controlled field names to virtual metadata field names and configurations
     */
    public Map<String, HashMap<String, AuthorityVirtualMetadataConfiguration>> getMap() {
        return map;
    }

}
