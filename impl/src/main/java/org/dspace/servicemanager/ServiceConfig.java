/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
