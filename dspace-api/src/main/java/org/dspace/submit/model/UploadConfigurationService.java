/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.util.Map;

/**
 * Simple bean to manage different Access Condition configuration
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class UploadConfigurationService {

    /**
     * Mapping the submission step process identifier with the configuration (see configuration at access-conditions
     * .xml)
     */
    private Map<String, UploadConfiguration> map;

    public Map<String, UploadConfiguration> getMap() {
        return map;
    }

    public void setMap(Map<String, UploadConfiguration> map) {
        this.map = map;
    }


}
