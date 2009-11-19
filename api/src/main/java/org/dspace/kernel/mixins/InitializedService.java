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
