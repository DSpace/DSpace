/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

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
