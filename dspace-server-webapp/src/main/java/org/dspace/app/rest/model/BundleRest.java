package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;

/**
 * The Bundle REST Resource
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */

public class BundleRest extends DSpaceObjectRest {
    public static final String NAME = "bundle";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private Bitstream primaryBitstream;
    private List<Bitstream> bitstreams;
    private List<Item> items;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Bitstream getPrimaryBitstream() {
        return primaryBitstream;
    }

    public void setPrimaryBitstream(Bitstream primaryBitstream) {
        this.primaryBitstream = primaryBitstream;
    }

    public List<Bitstream> getBitstreams() {
        return bitstreams;
    }

    public void setBitstreams(List<Bitstream> bitstreams) {
        this.bitstreams = bitstreams;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

}
