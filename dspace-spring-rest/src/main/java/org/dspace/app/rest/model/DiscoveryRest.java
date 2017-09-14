package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;

/**
 * Rest Resource for the Discovery endpoint
 */
public class DiscoveryRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }
}
