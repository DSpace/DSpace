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
public class DynamicLayoutBoxRelationConfigurationRest implements DynamicLayoutBoxConfigurationRest {


    public static final String NAME = "boxrelationconfiguration";

    @JsonProperty(value = "discovery-configuration")
    private String discoveryConfiguration;

    private String type = NAME;

    /**
     * Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the discovery configuration.
     */
    public String getDiscoveryConfiguration() {
        return discoveryConfiguration;
    }

    /**
     * Sets the discovery configuration.
     */
    public void setDiscoveryConfiguration(String configuration) {
        this.discoveryConfiguration = configuration;
    }
}
