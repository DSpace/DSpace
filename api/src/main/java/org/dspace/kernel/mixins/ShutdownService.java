/**
 * $Id: ShutdownService.java 3229 2008-10-23 13:13:58Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/kernel/mixins/ShutdownService.java $
 * DestroyedService.java - DSpace2 - Oct 23, 2008 12:03:43 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
