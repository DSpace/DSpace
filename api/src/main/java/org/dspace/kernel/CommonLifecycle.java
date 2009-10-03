/*
 * $Id: $
 * $URL: $
 * CommonLifecycle.java - DS2 - Feb 24, 2009 11:44:14 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.kernel;

/**
 * Beans that have a lifecycle and can be controlled via their lifecycle implement this interface.
 * Based on the Sakai K2 lifecycle interface -AZ
 * 
 * @param <T> the type of object managed by this lifecycle.
 */
public interface CommonLifecycle<T> {

    /**
     * Starts the bean. This initializes and causes the object to begin functioning.
     * Should not happen automatically when the object is created.
     */
    public void start();

    /**
     * Stops the bean. This turns the object off and causes related things to be shutdown.
     * Object should be able to be started again.
     */
    public void stop();

    /**
     * Gets a reference to the bean that is being managed inside this lifecycle.
     * @return the managed object
     */
    public T getManagedBean();

    /**
     * Destroy the managed bean entirely. It will be stopped first if not stopped and cannot be
     * started again afterwards.
     */
    public void destroy();

}
