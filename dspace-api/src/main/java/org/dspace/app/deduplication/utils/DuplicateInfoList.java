/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.List;

public class DuplicateInfoList {

    private long size;

    private List<DuplicateInfo> dsi;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<DuplicateInfo> getDsi() {
        return dsi;
    }

    public void setDsi(List<DuplicateInfo> dsi) {
        this.dsi = dsi;
    }

}