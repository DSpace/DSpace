/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel;

import java.util.List;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Allows for non-specific access to the core services.
 * No dependency on the underlying mechanism is exposed.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceManager {

    /**
     * Get the application context
     */
    public ConfigurableApplicationContext getApplicationContext();

    /**
     * Allows developers to get the desired service singleton by the provided type. <br>
     * This should return all instantiated objects of the type specified 
     * (may not all be singletons).
     * 
     * @param <T> Class type
     * @param type the type for the requested service (this will typically be the interface class but can be concrete as well)
     * @return the list of service singletons OR empty list if none is found
     */
    public <T> List<T> getServicesByType(Class<T> type);

    /**
     * Allows developers to get the desired service singleton by the provided name and type.
     * Provide {@code null} for the name if it is not known, but it is
     * better to ensure it is set.
     * <p>
     * <em>NOTE</em>: This also allows special access to the underlying
     * service manager objects.  If using Spring this allows access to the
     * underlying ApplicationContext object like so:<br>
     * {@code getServiceByName(ApplicationContext.class.getName(), ApplicationContext.class);}
     * If using Guice then the same applies like so:<br>
     * {@code getServiceByName(Injector.class.getName(), Injector.class);}
     * It is also possible to register a module and cause Guice to fill
     * in any injected core services (see register method).
     * </p>
     *
     * @param <T> Class type
     * @param name (optional) the unique name for this service.
     * If null then the bean will be returned if there is only one
     * service of this type.
     * @param type the type for the requested service (this will typically be the interface class but can be concrete as well)
     * @return the service singleton OR null if none is found
     */
    public <T> T getServiceByName(String name, Class<T> type);

    /**
     * Lookup to see if a service exists with the given name.
     * 
     * @param name the unique name for this service
     * @return true if it exists, false otherwise
     */
    public boolean isServiceExists(String name);

    /**
     * Get the names of all registered service singletons.  By
     * convention, the name typically matches the fully qualified class 
     * name).
     * 
     * @return the list of all current registered services
     */
    public List<String> getServicesNames();

    /**
     * Allows adding singleton services and providers in at runtime or 
     * after the service manager has started up.
     * This is primarily useful for registering providers, filters, and
     * plugins with the DSpace core.
     * <p>
     * <em>NOTE:</em> It is important that you also call
     * {@link #unregisterService(String)} if you are shutting
     * down the context (webapp, etc.) that registered the service so 
     * that the full lifecycle completes correctly.
     * </p>
     * <p>
     * <em>NOTE:</em> if using Guice it is possible to register a Guice
     * Module as a service, which will not actually register it but will 
     * cause anything in the Module to have existing core services injected
     * into it.  You can use anything as the name in this case.
     * </p>
     * 
     * @param name the name of the service (must be unique)
     * @param service the object to register as a singleton service
     * @throws IllegalArgumentException if the service cannot be registered
     */
    public void registerService(String name, Object service);

    public void registerServiceNoAutowire(String name, Object service);
    /**
     * Allows adding singleton services and providers in at runtime or 
     * after the service manager has started up.
     * This is the same as {@link #registerService(String, Object)}
     * except that it allows the core service manager to startup your 
     * service for you instead of you providing a service to the core.
     * In general, it is better if you use your own service manager 
     * (like Spring or Guice) to manage your services  and simply 
     * inherit the core service beans from the DSpace core service
     * manager using the special capabilities of
     * {@link #getServiceByName(String, Class)}.
     * 
     * @see ServiceManager#getServiceByName(String, Class)
     * @param <T> Class type
     * @param name the name of the service (must be unique)
     * @param type the class type of the service (must be in the current classloader)
     * @return the service class
     * @throws IllegalArgumentException if the service cannot be registered because the name is taken or type is invalid or other
     */
    public <T> T registerServiceClass(String name, Class<T> type);

    /**
     * Allows a service to be unregistered (which will only work if
     * nothing depends on it).
     * This is primarily used for providers, filters, plugins, etc. 
     * which were registered but are no longer available because the 
     * context they are running in is shutting down or restarting.
     * <br>
     * <em>WARNING: This should not be used to attempt to unregister core 
     * services as that will fail.</em>
     * 
     * @param name the name of the service (must be unique)
     * @throws IllegalArgumentException if the bean cannot be unregistered
     */
    public void unregisterService(String name);

    /**
     * Allows new configuration settings to be pushed into the core 
     * DSpace configuration.
     * These will cause a settings refresh action to be called for all
     * services which are listening and will cause any bean properties 
     * to be pushed into existing beans.
     * 
     * @param settings a map of keys (names) and values
     */
    public void pushConfig(Map<String, Object> settings);

}
