package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO TOM UNIT TEST
 */
public class SearchResultEntryRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private Map<String, List<String>> hitHighlights;

    @JsonIgnore
    private DSpaceObjectRest dspaceObject;


    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public Map<String, List<String>> getHitHighlights() {
        return hitHighlights;
    }

    public void addHitHighlights(String key, List<String> value) {
        if(hitHighlights == null) {
            hitHighlights = new HashMap<>();
        }
        hitHighlights.put(key, value);
    }

    public void setHitHighlights(final Map<String, List<String>> hitHighlights) {
        this.hitHighlights = hitHighlights;
    }

    public DSpaceObjectRest getDspaceObject() {
        return dspaceObject;
    }

    public void setDspaceObject(final DSpaceObjectRest dspaceObject) {
        this.dspaceObject = dspaceObject;
    }
}
