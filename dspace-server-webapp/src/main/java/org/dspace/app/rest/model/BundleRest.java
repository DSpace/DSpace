package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.content.Bitstream;

/**
 * The Bundle REST Resource
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */

public class BundleRest extends DSpaceObjectRest {
    public static final String NAME = "bundle";
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
