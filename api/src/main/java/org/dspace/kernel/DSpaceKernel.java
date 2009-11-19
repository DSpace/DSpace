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
package org.dspace.kernel;

import org.dspace.services.ConfigurationService;

/**
 * The interface of the Kernel,
 * this is the most core piece of the system and initalizing this will startup the dspace core
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
