/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
