/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;


/**
 * Allow the service to be notified when the service manager is shutting 
 * it down.  This will typically be called when the kernel is stopped or
 * destroyed.  Any cleanup that a service needs to do when it is 
 * shut down should happen here.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ShutdownService {

    /**
     * Called as the service manager is stopping or shutting down.
     */
    public void shutdown();

}
