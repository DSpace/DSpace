/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.kernel.Activator;
import org.dspace.kernel.mixins.ConfigChangeListener;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ServiceChangeListener;
import org.dspace.kernel.mixins.ServiceManagerReadyAware;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.spring.SpringServiceManager;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This is the core service manager which ties together the other
 * service managers and generally handles any edge cases in the various 
 * systems.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceServiceManager implements ServiceManagerSystem {

    private static Logger log = LoggerFactory.getLogger(DSpaceServiceManager.class);

    private final DSpaceConfigurationService configurationService;

    protected boolean running = false;
    /**
     * @return true if the service manager is running
     */
    public boolean isRunning() {
        return running;
    }
    /**
     * Checks to see if the service manager is running, if not throws an exception
     * @throws IllegalStateException if not running
     */
    private void checkRunning() {
        if (! isRunning()) {
            throw new IllegalStateException("Cannot perform operations on a service manager that is not running");
        }
    }

    private List<ServiceManagerSystem> serviceManagers = Collections.synchronizedList(new ArrayList<ServiceManagerSystem>());
    private SpringServiceManager primaryServiceManager = null;
    /**
     * This holds the stack of activators.  It is randomly ordered.
     */
    private List<Activator> activators = Collections.synchronizedList(new ArrayList<Activator>());

    protected boolean developing = false;
    /**
     * Standard constructor.
     * @param configurationService current DSpace configuration service
     */
    public DSpaceServiceManager(DSpaceConfigurationService configurationService) {
        if (configurationService == null) {
            throw new IllegalArgumentException("Failure creating service manager, configuration service is null");
        }
        this.configurationService = configurationService;
        this.developing = configurationService.getPropertyAsType("service.manager.developing", boolean.class);
    }

    protected boolean testing = false;
    protected String[] springXmlConfigFiles = null;
    /**
     * TESTING - This is for testing only.
     * @param configurationService current DSpace configuration service
     * @param springXmlConfigFiles one or more Spring XML configs
     */
    protected DSpaceServiceManager(DSpaceConfigurationService configurationService, String... springXmlConfigFiles) {
        this.configurationService = configurationService;
        this.springXmlConfigFiles = springXmlConfigFiles;
        this.testing = true;
        this.developing = true;
    }

    /**
     * Registers the activators using this service manager.
     */
    private void registerActivators() {

        for (Activator activator : this.getServicesByType(Activator.class))
        {
             // succeeded creating the activator
            try {
                activator.start(this);
                activators.add(activator);
                log.info("Started and registered activator: " + activator.getClass().getName());
            } catch (Exception e1) {
                log.error("ERROR: Failed to start activator ("+ activator.getClass().getName() +"): " + e1.getMessage(), e1);
            }
        }
    }

    /**
     * Unregisters all registered activators using this service manager.
     */
    private void unregisterActivators() {
        for (Activator activator : activators) {
            if (activator != null) {
                String activatorClassName = activator.getClass().getName();
                // succeeded creating the activator
                try {
                    activator.stop(this);
                    log.info("Stopped and unregistered activator: " + activatorClassName);
                } catch (Exception e1) {
                    log.error("ERROR: Failed to stop activator ("+activatorClassName+"): " + e1.getMessage(), e1);
                }
            }
        }
        activators.clear();
    }

    /**
     * This will call all the services which want to be notified when the service manager is ready
     */
    public void notifyServiceManagerReady() {
        for (ServiceManagerSystem sms : serviceManagers) {
            List<ServiceManagerReadyAware> services = sms.getServicesByType(ServiceManagerReadyAware.class);
            for (ServiceManagerReadyAware serviceManagerReadyAware : services) {
                try {
                    serviceManagerReadyAware.serviceManagerReady(this);
                } catch (Exception e) {
                    log.error("ERROR: Failure in service when calling serviceManagerReady: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Checks to see if a listener should be notified
     * @param implementedTypes the types implemented by the service changing
     * @param serviceChangeListener the listener
     * @return true if it should be notified, false otherwise
     */
    private boolean checkNotifyServiceChange(List<Class<?>> implementedTypes,
            ServiceChangeListener serviceChangeListener) {
        boolean notify = false;
        Class<?>[] notifyTypes = serviceChangeListener.notifyForTypes();
        if (notifyTypes == null || notifyTypes.length == 0) {
            notify = true;
        } else {
            for (Class<?> notifyType : notifyTypes) {
                for (Class<?> implementedType : implementedTypes) {
                    if (notifyType.equals(implementedType)) {
                        notify = true;
                        break;
                    }
                }
            }
        }
        return notify;
    }

    /**
     * Shut down all service managers, including this one.
     */
    @Override
    public void shutdown() {
        unregisterActivators();
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                sms.shutdown();
            } catch (Exception e) {
                // shutdown failures are not great but should NOT cause an interruption of processing
                log.error("Failure shutting down service manager ("+sms+"): " + e.getMessage(), e);
            }
        }
        this.running = false; // wait til the end
        this.serviceManagers.clear();
        this.primaryServiceManager = null;
        log.info("Shutdown DSpace core service manager");
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#startup()
     */
    @Override
    public void startup() {
        if (!testing) {
            // try to load up extra config files for spring
            String[] extraConfigs = configurationService.getPropertyAsType("service.manager.spring.configs", String[].class);
            if (extraConfigs != null) {
                if (springXmlConfigFiles == null) {
                    springXmlConfigFiles = extraConfigs;
                } else {
                    springXmlConfigFiles = (String[])ArrayUtils.addAll(springXmlConfigFiles, extraConfigs);
                }
            }
        }
        try {
            // have to put this at the top because otherwise initializing beans will die when they try to use the SMS
            this.running = true;
            // create the primary SMS and start it
            SpringServiceManager springSMS = new SpringServiceManager(this, configurationService, testing, developing, springXmlConfigFiles);
            try {
                springSMS.startup();
            } catch (Exception e) {
                // startup failures are deadly
                throw new IllegalStateException("failure starting up spring service manager: " + e.getMessage(), e);
            }
            // add it to the list of service managers
            this.serviceManagers.add(springSMS);
            this.primaryServiceManager = springSMS;

            // now startup the activators
            registerActivators();

            // now we call the ready mixins
            notifyServiceManagerReady();
            
        } catch (Exception e) {
            shutdown(); // execute the shutdown
            String message = "Failed to startup the DSpace Service Manager: " + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void registerService(String name, Object service) {
        checkRunning();
        if (name == null || service == null) {
            throw new IllegalArgumentException("name and service cannot be null");
        }
        // register service/provider with all
        for (ServiceManagerSystem sms : serviceManagers) {
            sms.registerService(name, service);
        }
    }

    @Override
    public void registerServiceNoAutowire(String name, Object service) {
        checkRunning();
        if (name == null || service == null) {
            throw new IllegalArgumentException("name and service cannot be null");
        }
        // register service/provider with all
        for (ServiceManagerSystem sms : serviceManagers) {
            sms.registerServiceNoAutowire(name, service);
        }
    }

    public <T> T registerServiceClass(String name, Class<T> type) {
        checkRunning();
        if (name == null || type == null) {
            throw new IllegalArgumentException("name and type cannot be null");
        }
        // we only register with the primary
        return primaryServiceManager.registerServiceClass(name, type);
    }

    public void unregisterService(String name) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        // only unregister with the primary
        primaryServiceManager.unregisterService(name);
    }

    public <T> T getServiceByName(String name, Class<T> type) {
        checkRunning();
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        T service = null;
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                service = sms.getServiceByName(name, type);
                if (service != null) {
                    break;
                }
            } catch (Exception e) {
                // keep going
            }
        }
        // need to check the service mixin manager if not found
        if (service == null 
                && name != null) {
            for (ServiceManagerSystem sms : serviceManagers) {
                if (service == null) {
                    service = sms.getServiceByName(name, type);
                }
            }
        }
        return service;
    }

    @Override
    public ConfigurableApplicationContext getApplicationContext() {
        return primaryServiceManager.getApplicationContext();
    }

    public <T> List<T> getServicesByType(Class<T> type) {
        checkRunning();
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        HashSet<T> set = new HashSet<T>();
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                set.addAll( sms.getServicesByType(type) );
            } catch (Exception e) {
                // keep going
            }
        }
        // put the set into a list for easier access and sort it
        List<T> services = new ArrayList<T>(set);
        Collections.sort(services, new ServiceManagerUtils.ServiceComparator());
        return services;
    }

    public List<String> getServicesNames() {
        checkRunning();
        List<String> names = new ArrayList<String>();
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                names.addAll( sms.getServicesNames() );
            } catch (Exception e) {
                // keep going
            }
        }
        Collections.sort(names);
        return names;
    }

    public boolean isServiceExists(String name) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        boolean exists = false;
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                exists = sms.isServiceExists(name);
                if (exists) {
                    break;
                }
            } catch (Exception e) {
                // keep going
            }
        }
        return exists;
    }

    public Map<String, Object> getServices() {
        checkRunning();
        Map<String, Object> services = new HashMap<String, Object>();
        for (ServiceManagerSystem sms : serviceManagers) {
            try {
                for (Entry<String, Object> entry : sms.getServices().entrySet()) {
                    if (! services.containsKey(entry.getKey())) {
                        services.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                // keep going if it fails for one
                log.error("Failed to get list of services from service manager ("+sms.getClass()+"): " + e.getMessage(), e);
            }
        }
        return services;
    }

    /*
     * Handles the configuration push for all services.
     * Every service gets called to notify them of the config change
     * depending on the the listener they are using.
     */
    public void pushConfig(Map<String, Object> properties) {
        checkRunning();
        if (properties != null && !properties.isEmpty()) {
            // load in the new settings to the config service
            String[] changedNames = configurationService.loadConfiguration(properties);
            if (changedNames.length > 0) {
                // some configs changed so push the changes to the listeners in all known services and providers
                // make the list of changed setting names and map of changed settings
                ArrayList<String> changedSettingNames = new ArrayList<String>();
                Map<String, String> changedSettings = new LinkedHashMap<String, String>();
                for (String configName : changedNames) {
                    changedSettingNames.add(configName);
                    changedSettings.put( configName, configurationService.getProperty(configName) );
                }
                // notify the services that implement the mixin
                for (ServiceManagerSystem sms : serviceManagers) {
                    List<ConfigChangeListener> configChangeListeners = sms.getServicesByType(ConfigChangeListener.class);
                    for (ConfigChangeListener configChangeListener : configChangeListeners) {
                        // notify this service
                        try {
                            boolean notify = false;
                            String[] notifyNames = configChangeListener.notifyForConfigNames();
                            if (notifyNames == null || notifyNames.length == 0) {
                                notify = true;
                            } else {
                                for (String notifyName : notifyNames) {
                                    // check to see if the name matches one of those the listener cares about
                                    for (String changedName : changedNames) {
                                        if (notifyName != null && notifyName.equals(changedName)) {
                                            notify = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            // do the notify if we should at this point
                            if (notify) {
                                configChangeListener.configurationChanged(changedSettingNames, changedSettings);
                            }
                        } catch (Exception e) {
                            log.error("Failure occurred while trying to notify service of config change: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    // STATICS

    /**
     * Configures a given service (i.e. bean) based on any DSpace configuration
     * settings which refer to it by name. .
     * <P>
     * NOTE: Any configurations related to a specific service MUST be prefixed
     * with the given service's name (e.g. [serviceName].setting = value)
     * <P>
     * This method logs an error if it encounters configs which refer to a
     * service by name, but is an invalid setting for that service.
     * 
     * @param serviceName the name of the service
     * @param service the service object (which will be configured)
     * @param config the running configuration service
     */
    public static void configureService(String serviceName, Object service, ConfigurationService config) {

        // Check if the configuration has any properties whose prefix
        // corresponds to this service's name
        List<String> configKeys = config.getPropertyKeys(serviceName);
        if(configKeys!=null && !configKeys.isEmpty())
        {
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(service);
            for (String key : configKeys) {
                // Remove serviceName prefix from key. This is the name of the actual bean's parameter
                // This removes the first x chars, where x is length of serviceName + 1 char
                // Format of Key: [serviceName].[param]
                String param = key.substring(serviceName.length() + 1);

                try {
                    // Attempt to set this configuration on the given service's bean
                    beanWrapper.setPropertyValue(param, config.getProperty(key));
                    log.info("Set param ("+param+") on service bean ("+serviceName+") to: " + config.getProperty(key));
                } catch (RuntimeException e) {
                    // If an error occurs, just log it
                    log.error("Unable to set param ("+param+") on service bean ("+serviceName+") to: " + config.getProperty(key), e);
                }
            }
        }
    }

    /**
     * Initializes a service if it asks to be initialized or does nothing.
     * @param service any bean
     * @throws IllegalStateException if the service init fails
     */
    public static void initService(Object service) {
        if (service instanceof InitializedService) {
            try {
                ((InitializedService) service).init();
            } catch (Exception e) {
                throw new IllegalStateException("Failure attempting to initialize service (" + service + "): " + e.getMessage(), e);
            }
        }
    }

    /**
     * Shuts down a service if it asks to be shutdown or does nothing.
     * @param service any bean
     */
    public static void shutdownService(Object service) {
        if (service instanceof ShutdownService) {
            try {
                ((ShutdownService) service).shutdown();
            } catch (Exception e) {
                log.error("Failure shutting down service: " + service, e);
            }
        }
    }

}
