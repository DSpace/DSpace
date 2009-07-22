/**
 * $Id: InitializedService.java 3229 2008-10-23 13:13:58Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/kernel/mixins/InitializedService.java $
 * InitializingService.java - DSpace2 - Oct 23, 2008 12:00:01 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.kernel.mixins;

/**
 * This service mixin will cause the service/provider/etc. to be initialized when it is started by the service
 * manager, after all injections are complete the init method will be called,
 * any initialization that a service needs to do should happen here
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface InitializedService {

    /**
     * Executed after the service is created and has had all dependencies and configurations injected
     */
    public void init();

}
