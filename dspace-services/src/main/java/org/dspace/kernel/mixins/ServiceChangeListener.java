/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;

import java.util.List;

/**
 * Allow a service to be notified when other services change.
 * This is useful for keeping an eye on changing providers, filters, and
 * other services which drop in and out.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceChangeListener {

    /**
     * Allows filtering so that notifications are only sent when classes 
     * implement one of the types specified.  The listener method will
     * only be called once regardless of how many types match.
     * <p>
     * Just return null or empty array to be notified for all service 
     * registrations.
     * 
     * @return an array of classes OR null to be notified of all service registrations
     */
    public Class<?>[] notifyForTypes();

    /**
     * This will be called when services are newly registered with the 
     * service manager.  It will not be called when the core services
     * are starting up though.
     * <p>
     * It is not called until the service is fully initialized.
     * It is called once and only once per service that is registered.
     * 
     * @param serviceName the name of the service
     * @param service the service bean
     * @param implementedTypes a list of all the class types which this service implements
     */
    public void serviceRegistered(String serviceName, Object service, List<Class<?>> implementedTypes);

    /**
     * This will be called when services are removed from the service 
     * manager.
     * Services which are replaced will not have this method called and
     * will only receive
     * {@link #serviceRegistered(String, Object, List)}.
     * <p>
     * It is called immediately before the service is completely destroyed
     * so that the service object is still valid.
     * 
     * @param serviceName the name of the service
     * @param service the service bean
     */
    public void serviceUnregistered(String serviceName, Object service);

}
