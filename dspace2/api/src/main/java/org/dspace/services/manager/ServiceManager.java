/**
 * $Id$
 * $URL$
 * ServiceManager.java - Dspace - Sep 1, 2008 4:51:57 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services.manager;

import java.util.List;

/**
 * This is the interface for the service manager which allows for non-specific access to the core services,
 * no dependency on the underlying mechanism is exposed
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManager {

    /**
     * Allows developers to get the desired service singleton by the provided type
     * 
     * @param <T>
     * @param interfaceClass the type for the requested service (this will typically by the interface class)
     * @return the service singleton OR null if none is found
     */
    public <T> T getServiceByType(Class<T> interfaceClass);

    /**
     * Allows developers to get the desired service singleton by the provided name and type
     * 
     * @param <T>
     * @param name the unique name for this service
     * @param interfaceClass the type for the requested service (this will typically by the interface class)
     * @return the service singleton OR null if none is found
     */
    public <T> T getServiceByName(String name, Class<T> interfaceClass);

    /**
     * Lookup to see if a service exists with the given name
     * 
     * @param name the unique name for this service
     * @return true if it exists, false otherwise
     */
    public boolean isServiceExists(String name);

    /**
     * Get the names of all registered service singletons (by convention, the name typically matches the fully qualified class name)
     * @return the list of all current registered services
     */
    public List<String> getServicesNames();

    /**
     * @return the list of the types of all registered services
     */
    public List<Class<?>> getServicesTypes();

    /**
     * Gets all the current registered service singletons
     * 
     * @return the list of all current registered service singletons
     */
    public List<?> getServices();

}
