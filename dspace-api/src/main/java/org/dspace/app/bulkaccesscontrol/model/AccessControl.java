/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.model;

public class AccessControl {

    AccessConditionItem item;

    AccessConditionBitstream bitstream;

    public AccessControl() {
    }

    public AccessControl(AccessConditionItem item,
                         AccessConditionBitstream bitstream) {
        this.item = item;
        this.bitstream = bitstream;
    }

    public AccessConditionItem getItem() {
        return item;
    }

    public void setItem(AccessConditionItem item) {
        this.item = item;
    }

    public AccessConditionBitstream getBitstream() {
        return bitstream;
    }

    public void setBitstream(AccessConditionBitstream bitstream) {
        this.bitstream = bitstream;
    }
}
