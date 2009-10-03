/**
 * $Id: ServiceManagerSystem.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/ServiceManagerSystem.java $
 * ServiceManagerSystem.java - DSpace2 - Oct 5, 2008 3:23:22 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
 */

package org.dspace.servicemanager;

import java.util.Map;

import org.dspace.kernel.ServiceManager;

/**
 * This interface should be implemented by any service managers that we are using in the system,
 * e.g. Spring, Guice
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManagerSystem extends ServiceManager {

    /**
     * Startup the service manager and initialize all services
     */
    public void startup();

    /**
     * Shuts down the service manager and all services that it is managing
     */
    public void shutdown();

    /**
     * @return a map of name -> bean for all services that are currently known
     */
    public Map<String, Object> getServices();

}
