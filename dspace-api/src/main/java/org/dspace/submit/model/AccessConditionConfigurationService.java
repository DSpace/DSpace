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
 * Simple bean to manage different Access Condition configurations
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionConfigurationService {

    /**
     * Mapping the submission step process identifier with the configuration
     * (see configuration at access-conditions.xml)
     */
    private Map<String, AccessConditionConfiguration> map;

    public Map<String, AccessConditionConfiguration> getMap() {
        return map;
    }

    public void setMap(Map<String, AccessConditionConfiguration> map) {
        this.map = map;
    }

}