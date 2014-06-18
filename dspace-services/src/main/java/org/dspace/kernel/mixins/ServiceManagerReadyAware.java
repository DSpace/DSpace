/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;

import org.dspace.kernel.ServiceManager;

/**
 * Allow a service to be notified when the service manager has started
 * up all known services (including activators).  This is useful if a
 * service wants to do lookups of all known services or providers of a 
 * certain type when the system startup is complete.  It can also be 
 * used as a way to execute some code when the service manager startup
 * is complete.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManagerReadyAware {

    /**
     * This is called when the startup of all core services and 
     * activators is complete.
     * <p>
     * WARNING: This will never be called for providers which are 
     * registered with a service manager which is already running!
     * 
     * @param serviceManager the completely started service manager
     */
    public void serviceManagerReady(ServiceManager serviceManager);

}
