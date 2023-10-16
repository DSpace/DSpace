/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.List;

/**
 * Java Bean to expose the COAR Notify Section during in progress submission.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class DataCOARNotify implements SectionData {

    private String pattern;
    private List<Integer> services;

    public DataCOARNotify() {

    }

    public DataCOARNotify(String pattern, List<Integer> services) {
        this.pattern = pattern;
        this.services = services;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<Integer> getServices() {
        return services;
    }

    public void setServices(List<Integer> services) {
        this.services = services;
    }
}
