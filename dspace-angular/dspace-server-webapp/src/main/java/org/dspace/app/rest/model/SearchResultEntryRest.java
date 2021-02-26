/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to create a container for the information in the SearchResultEntryResource
 */
public class SearchResultEntryRest extends RestAddressableModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private Map<String, List<String>> hitHighlights;

    private RestAddressableModel indexableObject;

    @JsonIgnore
    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    @JsonIgnore
    public Class getController() {
        return DiscoveryRestController.class;
    }

    public Map<String, List<String>> getHitHighlights() {
        return hitHighlights;
    }

    public void addHitHighlights(String key, List<String> value) {
        if (hitHighlights == null) {
            hitHighlights = new HashMap<>();
        }
        hitHighlights.put(key, value);
    }

    public void setHitHighlights(final Map<String, List<String>> hitHighlights) {
        this.hitHighlights = hitHighlights;
    }

    @JsonIgnore
    public RestAddressableModel getIndexableObject() {
        return indexableObject;
    }

    public void setIndexableObject(final RestAddressableModel indexableObject) {
        this.indexableObject = indexableObject;
    }
}
