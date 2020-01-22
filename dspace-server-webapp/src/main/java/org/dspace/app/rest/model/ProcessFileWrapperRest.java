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

public class ProcessFileWrapperRest {
    private Integer processId;

    @JsonIgnore
    private List<BitstreamRest> bitstreams;

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public void setBitstreams(List<BitstreamRest> bistreams) {
        this.bitstreams = bistreams;
    }

    public List<BitstreamRest> getBitstreams() {
        return bitstreams;
    }
}
