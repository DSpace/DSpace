/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.List;

import org.dspace.app.rest.model.NotifyServiceRest;

/**
 * Java Bean to expose the section creativecommons representing the CC License during in progress submission.
 */
public class DataCOARNotify implements SectionData {

    private String pattern;
    private List<NotifyServiceRest> services;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<NotifyServiceRest> getServices() {
        return services;
    }

    public void setServices(List<NotifyServiceRest> services) {
        this.services = services;
    }
}
