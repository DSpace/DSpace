/**
 * $Id: ServiceConfig.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/ServiceConfig.java $
 * ServiceConfig.java - DSpace2 - Oct 5, 2008 8:08:22 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.servicemanager;

import org.dspace.servicemanager.config.DSpaceConfig;



/**
 * This holds the settings for a service config triple,
 * can create this from a {@link DSpaceConfig} object
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ServiceConfig {

    private String paramName;
    private String serviceName;
    private Object value;

    public ServiceConfig(String paramName, String serviceName, Object value) {
        if (paramName == null || serviceName == null) {
            throw new IllegalArgumentException("paramName and serviceName must not be null");
        }
        this.paramName = paramName;
        this.serviceName = serviceName;
        this.value = value;
    }

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
