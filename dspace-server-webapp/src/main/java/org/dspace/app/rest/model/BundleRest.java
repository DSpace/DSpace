/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Bundle REST Resource
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@LinksRest(links = {
        @LinkRest(
                name = BundleRest.BITSTREAMS,
                method = "getBitstreams"
        ),
        @LinkRest(
                name = BundleRest.PRIMARY_BITSTREAM,
                method = "getPrimaryBitstream"
        )
})
public class BundleRest extends DSpaceObjectRest {
    public static final String NAME = "bundle";
    public static final String PLURAL_NAME = "bundles";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String BITSTREAMS = "bitstreams";
    public static final String PRIMARY_BITSTREAM = "primaryBitstream";

    @Override
    @JsonIgnore
    public String getId() {
        return super.getId();
    }

    public String getCategory() {
        return CATEGORY;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }
}
