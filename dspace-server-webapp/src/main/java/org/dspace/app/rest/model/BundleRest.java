/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.content.Bitstream;

/**
 * The Bundle REST Resource
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */

public class BundleRest extends DSpaceObjectRest {
    public static final String NAME = "bundle";
    public static final String PLURAL_NAME = "bundles";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private BitstreamRest primaryBitstream;
    private List<BitstreamRest> bitstreams;

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

    @JsonIgnore
    @LinkRest(linkClass = Bitstream.class)
    public BitstreamRest getPrimaryBitstream() {
        return primaryBitstream;
    }

    public void setPrimaryBitstream(BitstreamRest primaryBitstream) {
        this.primaryBitstream = primaryBitstream;
    }

    @LinkRest(linkClass = Bitstream.class)
    @JsonIgnore
    public List<BitstreamRest> getBitstreams() {
        return bitstreams;
    }

    public void setBitstreams(List<BitstreamRest> bitstreams) {
        this.bitstreams = bitstreams;
    }

}
