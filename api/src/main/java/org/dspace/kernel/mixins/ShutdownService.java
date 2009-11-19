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
package org.dspace.kernel.mixins;


/**
 * This service mixin will cause the service/provider/etc. to be shutdown when the service manager is shutting down the service,
 * this will typically be called when the kernel is stopped or destroyed,
 * any cleanup that a service needs to do when it is shutdown should happen here
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ShutdownService {

    /**
     * Called as the service manager is stopping or shutting down
     */
    public void shutdown();

}
