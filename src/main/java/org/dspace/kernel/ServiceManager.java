/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/kernel/ServiceManager.java $
 * 
 * $Revision: 3242 $
 * 
 * $Date: 2008-10-27 09:07:03 -0700 (Mon, 27 Oct 2008) $
 *
 * Copyright (c) 2008, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its 
 * contributors may be used to endorse or promote products derived from 
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.kernel;

import java.util.List;
import java.util.Map;

/**
 * This is the interface for the service manager which allows for non-specific access to the core services,
 * no dependency on the underlying mechanism is exposed
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManager {

    /**
     * Allows developers to get the desired service singleton by the provided type <br/>
     * This should return all instantiated objects of the type specified (may not all be singletons)
     * 
     * @param <T>
     * @param type the type for the requested service (this will typically be the interface class but can be concrete as well)
     * @return the list of service singletons OR empty list if none is found
     */
    public <T> List<T> getServicesByType(Class<T> type);

    /**
     * Allows developers to get the desired service singleton by the provided name and type,
     * can provide null for the name if it is not known but it is better to ensure it is set <br/>
     * NOTE: This also allows special access to the underlying service manager objects,
     * if using spring this allows access to the underlying ApplicationContext object like so: <br/>
     * getServiceByName(ApplicationContext.class.getName(), ApplicationContext.class);
     * if using Guice then the same applies like so: <br/>
     * getServiceByName(Injector.class.getName(), Injector.class);
     * it is also possible to register a module and cause Guice to fill in any injected core services (see register method)
     * 
     * @param <T>
     * @param name (optional) the unique name for this service, 
     * if null then the bean will be returned if there is only one service of this type 
     * @param type the type for the requested service (this will typically be the interface class but can be concrete as well)
     * @return the service singleton OR null if none is found
     */
    public <T> T getServiceByName(String name, Class<T> type);

    /**
     * Lookup to see if a service exists with the given name
     * 
     * @param name the unique name for this service
     * @return true if it exists, false otherwise
     */
    public boolean isServiceExists(String name);

    /**
     * Get the names of all registered service singletons (by convention, 
     * the name typically matches the fully qualified class name)
     * 
     * @return the list of all current registered services
     */
    public List<String> getServicesNames();

    /**
     * Allows adding singleton services and providers in at runtime or after the service manager has started up,
     * this is primarily useful for registering providers, filters, and plugins with the DSpace core <br/>
     * NOTE: It is important that you also call {@link #unregisterService(String)} if you are shutting
     * down the context (webapp, etc.) that registered the service so that the full lifecycle completes
     * correctly <br/>
     * NOTE: if using Guice it is possible to register a Guice Module as a service which will not 
     * actually register it but will cause anything in the Module to have existing core services
     * injected into it, you can use anything as the name in this case
     * 
     * @param name the name of the service (must be unique)
     * @param service the object to register as a singleton service
     * @throws IllegalArgumentException if the service cannot be registered
     */
    public void registerService(String name, Object service);

    /**
     * Allows adding singleton services and providers in at runtime or after the service manager has started up,
     * this is the same as {@link #registerService(String, Object)} except that it allows the core service
     * manager to startup your service for you instead of you providing a service to the core.
     * In general, it is better if you use your own service manager (like Spring or Guice) to manage your services
     * and simply inherit the core service beans from the DSpace core service manager using the special 
     * capabilities of {@link #getServiceByName(String, Class)}
     * 
     * @see ServiceManager#getServiceByName(String, Class)
     * @param name the name of the service (must be unique)
     * @param type the class type of the service (must be in the current classloader)
     * @throws IllegalArgumentException if the service cannot be registered because the name is taken or type is invalid or other
     */
    public <T> T registerServiceClass(String name, Class<T> type);

    /**
     * Allows a service to be unregistered (this will only work if nothing depends on it),
     * this is primarily used for providers, filters, plugins, etc. which were registered
     * but are no longer available because the context they are running in is shutting down or restarting <br/>
     * WARNING: This should not be used to attempt to unregister core services as that will fail
     * 
     * @param name the name of the service (must be unique)
     * @throws IllegalArgumentException if the bean cannot be unregistered
     */
    public void unregisterService(String name);

    /**
     * Allows new configuration settings to be pushed into the core DSpace configuration,
     * these will cause a settings refresh action to be called for all services which are listening
     * and will cause any bean properties to be pushed into existing beans
     * 
     * @param settings a map of keys (names) and values
     */
    public void pushConfig(Map<String, String> settings);

}
