/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.util.Map;

import org.dspace.app.rest.submit.factory.impl.PatchOperation;

/**
 * Class to mantain mapping configuration for PATCH operation needed by the Submission process
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class PatchConfigurationService {

    private Map<String, Map<String, PatchOperation>> map;

    public Map<String, Map<String, PatchOperation>> getMap() {
        return map;
    }

    public void setMap(Map<String, Map<String, PatchOperation>> map) {
        this.map = map;
    }

}
