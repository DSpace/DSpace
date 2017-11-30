/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;

/**
 * Allow the service or provider to be initialized when it is started
 * by the service manager.  After all injections are complete the init
 * method will be called.  Any initialization that a service needs to do 
 * should happen here.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface InitializedService {

    /**
     * Executed after the service is created and all dependencies and 
     * configurations injected.
     */
    public void init();

}
