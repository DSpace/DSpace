/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import org.dspace.servicemanager.config.DSpaceConfig;



/**
 * This holds the settings for a service config triple.
 * Create this from a {@link DSpaceConfig} object, or by name and value.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ServiceConfig {

    private String paramName;
    private String serviceName;
    private Object value;

    /** Construct from scratch. */
    public ServiceConfig(String paramName, String serviceName, Object value) {
        if (paramName == null || serviceName == null) {
            throw new IllegalArgumentException("paramName and serviceName must not be null");
        }
        this.paramName = paramName;
        this.serviceName = serviceName;
        this.value = value;
    }

    /** Construct from an existing DSpaceConfig. */
    public ServiceConfig(DSpaceConfig dspaceConfig) {
        this.paramName = dspaceConfig.getBeanProperty();
        this.serviceName = dspaceConfig.getBeanName();
        if (paramName == null || serviceName == null) {
            throw new IllegalArgumentException("paramName and serviceName must not be null");
        }
        this.value = dspaceConfig.getValue();
    }

    public String getParamName() {
        return paramName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Object getValue() {
        return value;
    }

}
