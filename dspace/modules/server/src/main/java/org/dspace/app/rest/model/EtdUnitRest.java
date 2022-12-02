package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.dspace.app.rest.RestResourceController;

/**
 * The Unit REST Resource
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@LinksRest(links = {
        @LinkRest(name = EtdUnitRest.COLLECTIONS, method = "getCollections")
})
public class EtdUnitRest extends DSpaceObjectRest {
    public static final String NAME = "etdunit";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String ETDUNITS = "etdunits";
    public static final String COLLECTIONS = "collections";

    private String name;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
