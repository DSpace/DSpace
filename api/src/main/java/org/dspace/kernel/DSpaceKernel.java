/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.kernel;

import org.dspace.services.ConfigurationService;

/**
 * This is the most core piece of the system:  instantiating one will
 * startup the dspace services framework.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface DSpaceKernel {

    public static final String KERNEL_NAME = "Kernel";
    public static final String MBEAN_PREFIX = "org.dspace:name=";
    public static final String MBEAN_SUFFIX = ",type=DSpaceKernel";
    public static final String MBEAN_NAME = MBEAN_PREFIX+KERNEL_NAME+MBEAN_SUFFIX;

    /**
     * @return the unique MBean name of this DSpace Kernel
     */
    public String getMBeanName();

    /**
     * @return true if this Kernel is started and running
     */
    public boolean isRunning();

    /**
     * @return the DSpace service manager instance for this Kernel
     */
    public ServiceManager getServiceManager();

    /**
     * @return the DSpace configuration service for this Kernel
     */
    public ConfigurationService getConfigurationService();

}
