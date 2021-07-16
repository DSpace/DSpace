/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.info;

import java.util.List;

public class Info {

    private List<Canvas> canvases;
    private List<Range> structures;
    private GlobalDefaults globalDefaults;

    public GlobalDefaults getGlobalDefaults() {
        return globalDefaults;
    }

    public void setGlobalDefaults(GlobalDefaults globalDefaults) {
        this.globalDefaults = globalDefaults;
    }

    public void setCanvases(List<Canvas> canvases) {
        this.canvases = canvases;
    }

    public List<Canvas> getCanvases() {
        return this.canvases;
    }

    public void setStructures(List<Range> structures) {
        this.structures = structures;
    }

    public List<Range> getStructures() {
        return structures;
    }

}
