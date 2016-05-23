/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import java.util.Map;

import org.dspace.kernel.ServiceManager;

/**
 * This interface should be implemented by any service managers that we are using in the system,
 * e.g. Spring, Guice.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManagerSystem extends ServiceManager {

    /**
     * Startup the service manager and initialize all services.
     */
    public void startup();

    /**
     * Shuts down the service manager and all services that it is managing.
     */
    public void shutdown();

    /**
     * Map service names to their beans.
     *
     * @return a map of name to bean for all services that are currently known
     */
    public Map<String, Object> getServices();

}
