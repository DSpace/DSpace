/**
 * $Id: ServiceChangeListener.java 3242 2008-10-27 16:07:03Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/kernel/mixins/ServiceChangeListener.java $
 * ServiceChangeListener.java - DSpace2 - Oct 24, 2008 6:39:29 PM - azeckoski
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

import java.util.List;

/**
 * This service manager mixin will allow a service to be notified when other services change,
 * this is useful for keeping an eye on changing providers, filters, and other services which
 * drop in and out
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceChangeListener {

    /**
     * Allows filtering so that notifications are only sent when classes implement
     * or are of one of the types specified, the listener method will only
     * be called once regardless of how many types match <br/>
     * Just return null or empty array to be notified for all service registrations
     * 
     * @return an array of classes OR null to be notified of all service registrations
     */
    public Class<?>[] notifyForTypes();

    /**
     * This will be called when services are newly registered with the service manager
     * (will not be called when the core services are starting up though) <br/>
     * It is not called until the service is fully initialized and
     * is called once and only once per service that is registered <br/>
     * 
     * @param serviceName the name of the service
     * @param service the service bean
     * @param implementedTypes a list of all the class types which this service implements
     */
    public void serviceRegistered(String serviceName, Object service, List<Class<?>> implementedTypes);

    /**
     * This will be called when services are removed from the service manager,
     * services which are replaced will not have this method called and will only call
     * {@link #serviceRegistered(String, Object, List)} <br/>
     * It is called immediately before the service is completely destroyed
     * so that the service object is still valid <br/>
     * 
     * @param serviceName the name of the service
     * @param service the service bean
     */
    public void serviceUnregistered(String serviceName, Object service);

}
