/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel;


/**
 * An activator is a special type which allows a provider to be plugged into the system by dropping a jar file
 * in with the kernel and adding in a hook in the configuration file. Activators are started after the
 * initial classes and the service manager have already been started. All classes which implement this
 * must have a public empty constructor (takes no parameters) (e.g.
 * {@code public MyClass() {}} )
 * <p>
 * If you want the system to execute your class then you must list it in 
 * the DSpace configuration with the fully qualified classpath
 * (NOTE that the xxx can be anything as long as it is unique): <br>
 * {@code activator.xxx = org.dspace.MyClass}
 * </p>
 * <p>
 * {@link #start(ServiceManager)} will be called after the class is created during kernel startup. 
 * Developers should create their providers/plugins/etc. in this method and
 * use the registration methods in the {@link ServiceManager} to register them. 
 * {@link #stop(ServiceManager)} will be called when the kernel shuts down. Perform any cleanup/shutdown actions
 * you like during this phase (unregistering your services here is a good idea). <br>
 * </p>
 * <p>This is modeled after the OSGi {@code BundleActivator}.</p>
 * <p>
 * There is another type of activator used in DSpace but it is 
 * configured via the configuration service only.  The class activator
 * is configured by creating a config property like this
 * (NOTE that the xxx can be anything as long as it is unique): <br>
 * {@code activator.class.xxx = org.dspace.MyClass;org.dspace.MyServiceName;constructor}<br>
 * Unlike the normal activators, these are started up when the kernel 
 * core services start and thus can actually be accessed from the 
 * service manager and referenced in providers and plugins.
 * </p>
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface Activator {

    /**
     * This is called when the service manager is starting this activator.
     * It is only called once.
     * It will be called after the core services are started. The ClassLoader used will be the one
     * that this class is associated with to ensure all dependencies are available.
     * <p>
     * This method should be used to startup and register services in most cases but it can be used
     * to simply perform some system startup actions if desired.
     * <p>
     * Exceptions thrown out of this method will not cause the system startup to fail.
     * 
     * @param serviceManager the current system service manager
     */
    public void start(ServiceManager serviceManager);

    /**
     * This is called when the service manager is shutting down this 
     * activator.  It is only called once.
     * It will be called before the core services are stopped. The ClassLoader used will be the one
     * that this class is associated with to ensure all dependencies are available.
     * <p>
     * This method should be used to shutdown and unregister services in most cases but it can be used
     * to simply perform some system shutdown actions if desired.
     * <p>
     * Exceptions thrown out of this method will not cause the system shutdown to fail.
     * <p>
     * WARNING: this can hang the shutdown by performing operations that 
     * take a long long time or are deadlocked.  The developer is
     * expected to ensure this does not happen.
     * 
     * @param serviceManager the current system service manager
     */
    public void stop(ServiceManager serviceManager);

}
