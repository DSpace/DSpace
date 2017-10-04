package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchSupportRest extends BaseObjectRest<String>{
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
