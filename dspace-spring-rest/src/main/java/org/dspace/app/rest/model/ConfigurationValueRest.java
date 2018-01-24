/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to store the information that'll be shown on the /search endpoint.
 */
public class ConfigurationValueRest extends BaseObjectRest<String> {

    public static final String NAME = "configuration";
    public static final String CATEGORY = RestModel.CONFIGURATION;
    @JsonIgnore
    private String scope;
    @JsonIgnore
    private String configurationName;

    private String key = "";
    private String value = "";

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getScope() {
        return scope;
    }
    public void setScope(String scope){
        this.scope = scope;
    }

    public String getConfigurationName() {
        return configurationName;
    }
    public void setConfigurationName(String configurationName){
        this.configurationName = configurationName;
    }

    public void setKey(String key){
        this.key = key;
    }
    public String getKey(){
        return key;
    }

    public void setValue(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }

    @Override
    public boolean equals(Object object){
        return (object instanceof ConfigurationValueRest &&
                new EqualsBuilder().append(this.getCategory(), ((ConfigurationValueRest) object).getCategory())
                        .append(this.getType(), ((ConfigurationValueRest) object).getType())
                        .append(this.getController(), ((ConfigurationValueRest) object).getController())
                        .append(this.getScope(), ((ConfigurationValueRest) object).getScope())
                        .append(this.getConfigurationName(), ((ConfigurationValueRest) object).getConfigurationName())
                        .append(this.getValue(), ((ConfigurationValueRest) object).getValue())
                        .isEquals());
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getCategory())
                .append(this.getType())
                .append(this.getController())
                .append(this.getScope())
                .append(this.getConfigurationName())
                .append(this.getValue())
                .toHashCode();
    }


}
