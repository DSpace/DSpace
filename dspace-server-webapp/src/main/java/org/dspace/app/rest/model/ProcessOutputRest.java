/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

public class ProcessOutputRest implements RestModel {
    public static final String NAME = "processOutput";
    private List<String> logs = null;

    public String getType() {
        return NAME;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public List<String> getLogs() {
        return logs;
    }
}
