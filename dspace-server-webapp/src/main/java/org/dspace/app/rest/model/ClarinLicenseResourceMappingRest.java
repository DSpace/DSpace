package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

import java.util.UUID;

@LinksRest(links = {
        @LinkRest(
                name = ClarinLicenseResourceMappingRest.CLARIN_LICENSE,
                method = "getClarinLicense"
        )
})
public class ClarinLicenseResourceMappingRest extends BaseObjectRest<Integer> {
    public static final String NAME = "clarinlicenseresourcemapping";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String CLARIN_LICENSE = "clarinLicense";

    public UUID bitstreamID;

    public ClarinLicenseResourceMappingRest() {
    }

    public UUID getBitstreamID() {
        return bitstreamID;
    }

    public void setBitstreamID(UUID bitstreamID) {
        this.bitstreamID = bitstreamID;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
