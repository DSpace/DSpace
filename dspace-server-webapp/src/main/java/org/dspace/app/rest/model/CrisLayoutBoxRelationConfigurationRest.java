/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutBoxRelationConfigurationRest implements CrisLayoutBoxConfigurationRest {


    public static final String NAME = "boxrelationconfiguration";

    @JsonProperty(value = "discovery-configuration")
    private String discoveryConfiguration;

    private String type = NAME;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDiscoveryConfiguration() {
        return discoveryConfiguration;
    }

    public void setDiscoveryConfiguration(String configuration) {
        this.discoveryConfiguration = configuration;
    }
}
