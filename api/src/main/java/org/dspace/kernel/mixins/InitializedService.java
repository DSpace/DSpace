/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
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
