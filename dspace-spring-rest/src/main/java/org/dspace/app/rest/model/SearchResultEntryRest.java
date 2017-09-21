package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;

import java.util.Map;

/**
 * TODO TOM UNIT TEST
 */
public class SearchResultEntryRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private Map<String, String> hitHighlights;
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

    public Map<String, String> getHitHighlights() {
        return hitHighlights;
    }

    public void setHitHighlights(final Map<String, String> hitHighlights) {
        this.hitHighlights = hitHighlights;
    }

    @LinkRest(linkClass = DSpaceObjectRest.class)
    public DSpaceObjectRest getDspaceObject() {
        return dspaceObject;
    }

    public void setDspaceObject(final DSpaceObjectRest dspaceObject) {
        this.dspaceObject = dspaceObject;
    }
}
