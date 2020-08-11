/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.kernel.Activator;
import org.dspace.kernel.config.SpringLoader;
import org.dspace.kernel.mixins.ConfigChangeListener;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ServiceChangeListener;
import org.dspace.kernel.mixins.ServiceManagerReadyAware;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.spring.DSpaceBeanFactoryPostProcessor;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A service locator based on Spring.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceServiceManager implements ServiceManagerSystem {

    private static Logger log = LoggerFactory.getLogger(DSpaceServiceManager.class);

    public static final String CONFIG_PATH = "spring/spring-dspace-applicationContext.xml";
    public static final String CORE_RESOURCE_PATH = "classpath*:spring/spring-dspace-core-services.xml";
    public static final String ADDON_RESOURCE_PATH = "classpath*:spring/spring-dspace-addon-*-services.xml";

    private final DSpaceConfigurationService configurationService;

    private ClassPathXmlApplicationContext applicationContext;

    protected boolean running = false;

    protected boolean developing = false;

    protected boolean testing = false;

    protected String[] springXmlConfigFiles = null;

    /**
     * This holds the stack of activators.  It is randomly ordered.
     */
    private final List<Activator> activators = Collections.synchronizedList(new ArrayList<>());

    /**
     * Standard constructor.
     *
     * @param configurationService current DSpace configuration service
     */
    public DSpaceServiceManager(DSpaceConfigurationService configurationService) {
        if (configurationService == null) {
            throw new IllegalArgumentException("Failure creating service manager:  configuration service is null");
        }
        this.configurationService = configurationService;
        this.developing = configurationService.getPropertyAsType("service.manager.developing", boolean.class);
    }

    /**
     * TESTING - This is for testing only.
     *
     * @param configurationService current DSpace configuration service.
     * @param springXmlConfigFiles one or more Spring XML configuration files.
     */
    protected DSpaceServiceManager(DSpaceConfigurationService configurationService,
            String... springXmlConfigFiles) {
        if (configurationService == null) {
            throw new IllegalArgumentException("Configuration service cannot be null");
        }
        this.configurationService = configurationService;
        this.springXmlConfigFiles = springXmlConfigFiles;
        this.testing = true;
        this.developing = true;
    }

    /**
     * @return true if the service manager is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Checks to see if the service manager is running.
     *
     * @throws IllegalStateException if not running
     */
    private void checkRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("Cannot perform operations on a service manager that is not running");
        }
    }

    /**
     * @return the current spring bean factory OR null if there is not one.
     */
    public ListableBeanFactory getBeanFactory() {
        if (applicationContext != null) {
            return applicationContext.getBeanFactory();
        }
        return null;
    }

    /**
     * @return the parent core Spring {@link ApplicationContext}
     */
    @Override
    public ClassPathXmlApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Registers the activators using this service manager.
     */
    private void registerActivators() {

        for (Activator activator : this.getServicesByType(Activator.class)) {
            // succeeded creating the activator
            try {
                activator.start(this);
                activators.add(activator);
                log.info("Started and registered activator: {}", activator.getClass().getName());
            } catch (Exception e1) {
                log.error(
                    "ERROR: Failed to start activator ({}): {}",
                        activator.getClass().getName(), e1.getMessage(), e1);
            }
        }
    }

    /**
     * De-registers all registered activators using this service manager.
     */
    private void unregisterActivators() {
        for (Activator activator : activators) {
            if (activator != null) {
                String activatorClassName = activator.getClass().getName();
                // succeeded creating the activator
                try {
                    activator.stop(this);
                    log.info("Stopped and unregistered activator: {}", activatorClassName);
                } catch (Exception e1) {
                    log.error("ERROR: Failed to stop activator ({}): {}",
                            activatorClassName, e1.getMessage(), e1);
                }
            }
        }
        activators.clear();
    }

    /**
     * This will call all the services which want to be notified when the service manager is ready.
     */
    public void notifyServiceManagerReady() {
        List<ServiceManagerReadyAware> services
                = getServicesByType(ServiceManagerReadyAware.class);
        for (ServiceManagerReadyAware serviceManagerReadyAware : services) {
            try {
                serviceManagerReadyAware.serviceManagerReady(this);
            } catch (Exception e) {
                log.error("ERROR: Failure in service when calling serviceManagerReady: {}",
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Checks to see if a listener should be notified.
     *
     * @param implementedTypes      the types implemented by the service changing
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
     * Shut down the Spring context and leave the "running" state.
     */
    @Override
    public void shutdown() {
        unregisterActivators();

        if (applicationContext != null) {
            try {
                applicationContext.close();
            } catch (Exception e) {
                // keep going anyway
                log.warn("Exception closing ApplicationContext:  {}", e.getMessage(), e);
            }
            try {
                applicationContext.destroy();
            } catch (Exception e) {
                // keep going anyway
                log.warn("Exception destroying ApplicationContext:  {}", e.getMessage(), e);
            }
            applicationContext = null;
        }

        this.running = false; // wait til the end
        log.info("DSpace service manager is shut down.");
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#startup()
     */
    @Override
    public void startup() {
        if (!testing) {
            // try to load up extra config files for spring
            String[] extraConfigs = configurationService
                .getPropertyAsType("service.manager.spring.configs", String[].class);
            if (extraConfigs != null) {
                if (springXmlConfigFiles == null) {
                    springXmlConfigFiles = extraConfigs;
                } else {
                    springXmlConfigFiles = (String[]) ArrayUtils.addAll(springXmlConfigFiles, extraConfigs);
                }
            }
        }

        long startTime = System.currentTimeMillis();
        try {
            // have to put this at the top because otherwise initializing beans will die when they try to use the SMS
            this.running = true;

            // get all Spring config paths
            String[] allPaths = getSpringPaths(testing, springXmlConfigFiles, configurationService);
            applicationContext = new ClassPathXmlApplicationContext(allPaths, false);
            // Make sure that the Spring files from the config directory can override the Spring files from our jars
            applicationContext.setAllowBeanDefinitionOverriding(true);
            applicationContext.setAllowCircularReferences(true);
            //applicationContext.registerShutdownHook(); // this interferes with the kernel shutdown hook
            // add the config interceptors (partially done in the xml)
            applicationContext.addBeanFactoryPostProcessor(
                    new DSpaceBeanFactoryPostProcessor(this, configurationService, testing));
            applicationContext.refresh();
            if (developing) {
                log.warn("Service Manager is running in developmentMode.  Services will be loaded on demand only");
                // TODO find a way to set this sucker to super duper lazy mode? it is currently not actually doing it
            } else {
                applicationContext.getBeanFactory().preInstantiateSingletons();
                applicationContext.getBeanFactory().freezeConfiguration();
            }

            // now startup the activators
            registerActivators();

            // now we call the ready mixins
            notifyServiceManagerReady();

        } catch (IllegalStateException e) {
            shutdown(); // execute the shutdown
            String message = "Failed to startup the DSpace Service Manager: " + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Service Manager started up in {} ms with {} services...",
                totalTime, applicationContext.getBeanDefinitionCount());
    }

    @Override
    public void registerService(String name, Object service) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        // register service/provider
        try {
            applicationContext.getBeanFactory().autowireBean(service);
        } catch (BeansException e) {
            throw new IllegalArgumentException(
                "Invalid service (" + service + ") with name (" + name + ") registration: " + e.getMessage(), e);
        }
        registerBean(name, service);
    }

    @Override
    public void registerServiceNoAutowire(String name, Object service) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        // register service/provider
        registerBean(name, service);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T registerServiceClass(String name, Class<T> type) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        T service;
        try {
            service = (T) applicationContext.getBeanFactory()
                    .autowire(type, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            registerBean(name, service);
        } catch (BeansException e) {
            throw new IllegalArgumentException("Invalid service class (" + type
                        + ") with name (" + name
                        + ") registration: " + e.getMessage(), e);
        }
        return service;
    }

    @Override
    public void unregisterService(String name) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        if (applicationContext.containsBean(name)) {
            try {
                Object beanInstance = applicationContext.getBean(name);
                try {
                    applicationContext.getBeanFactory().destroyBean(name, beanInstance);
                } catch (NoSuchBeanDefinitionException e) {
                    // this happens if the bean was registered manually (annoyingly)
                    DSpaceServiceManager.shutdownService(beanInstance);
                }
            } catch (BeansException e) {
                // nothing to do here, could not find the bean
            }
        }
    }

    /**
     * This handles the common part of various types of service registrations.
     *
     * @param name    name of bean
     * @param service service object
     */
    private void registerBean(String name, Object service) {
        try {
            applicationContext.getBeanFactory().initializeBean(service, name);
            applicationContext.getBeanFactory().registerSingleton(name, service);
        } catch (BeansException e) {
            throw new IllegalArgumentException(
                "Invalid service (" + service + ") with name (" + name + ") registration: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getServiceByName(String name, Class<T> type) {
        checkRunning();
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        T service = null;
        // handle special case to return the core AC
        if (ApplicationContext.class.getName().equals(name)
            && ApplicationContext.class.isAssignableFrom(type)) {
            service = (T) getApplicationContext();
        } else {
            if (name != null) {
                // get by name and type
                try {
                    service = (T) applicationContext.getBean(name, type);
                } catch (BeansException e) {
                    // no luck, try the fall back option
                    log.info(
                        "Unable to locate bean by name or id={}."
                                + " Will try to look up bean by type next."
                                + " BeansException: {}", name, e.getMessage());
                    service = null;
                }
            } else {
                // try making up the name based on the type
                try {
                    service = (T) applicationContext.getBean(type.getName(), type);
                } catch (BeansException e) {
                    // no luck, try the fall back option
                    log.info("Unable to locate bean by name or id={}."
                            + " Will try to look up bean by type next."
                            + " BeansException: {}", type.getName(), e.getMessage());
                    service = null;
                }
            }
            // if still no luck then try by type only
            if (name == null
                && service == null) {
                try {
                    Map<String, T> map = applicationContext.getBeansOfType(type);
                    if (map.size() == 1) {
                        // only return the bean if there is exactly one
                        service = (T) map.values().iterator().next();
                    } else {
                        log.error("Multiple beans of type {} found. Only one was expected!", type.getName());
                    }
                } catch (BeansException e) {
                    // I guess there are no beans of this type
                    log.error(e.getMessage(), e);
                    service = null;
                }
            }
        }
        return service;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getServicesByType(Class<T> type) {
        checkRunning();
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        List<T> services = new ArrayList<>();
        Map<String, T> beans;
        try {
            beans = applicationContext.getBeansOfType(type, true, true);
            services.addAll((Collection<? extends T>) beans.values());
        } catch (BeansException e) {
            throw new RuntimeException("Failed to get beans of type (" + type + "): " + e.getMessage(), e);
        }

        Collections.sort(services, new ServiceManagerUtils.ServiceComparator());
        return services;
    }

    @Override
    public List<String> getServicesNames() {
        checkRunning();
        ArrayList<String> beanNames = new ArrayList<>();
        String[] singletons = applicationContext.getBeanFactory().getSingletonNames();
        for (String singleton : singletons) {
            if (singleton.startsWith("org.springframework.context")) {
                continue; // skip the Spring standard ones
            }
            beanNames.add(singleton);
        }
        Collections.sort(beanNames);
        return beanNames;
    }

    @Override
    public boolean isServiceExists(String name) {
        checkRunning();
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        return applicationContext.containsBean(name);
    }

    @Override
    public Map<String, Object> getServices() {
        checkRunning();
        Map<String, Object> services = new HashMap<>();
        String[] singletons = applicationContext.getBeanFactory().getSingletonNames();
        for (String singleton : singletons) {
            if (singleton.startsWith("org.springframework.context")) {
                continue; // skip the spring standard ones
            }
            String beanName = singleton;
            Object service = applicationContext.getBeanFactory().getSingleton(beanName);
            if (service == null) {
                continue;
            }
            services.put(beanName, service);
        }
        return services;
    }

    /*
     * Handles the configuration push for all services.
     * Every service gets called to notify them of the config change
     * depending on the the listener they are using.
     */
    @Override
    public void pushConfig(Map<String, Object> properties) {
        checkRunning();
        if (properties != null && !properties.isEmpty()) {
            // load in the new settings to the config service
            String[] changedNames = configurationService.loadConfiguration(properties);
            if (changedNames.length > 0) {
                // some configs changed so push the changes to the listeners in all known services and providers
                // make the list of changed setting names and map of changed settings
                ArrayList<String> changedSettingNames = new ArrayList<>();
                Map<String, String> changedSettings = new LinkedHashMap<>();
                for (String configName : changedNames) {
                    changedSettingNames.add(configName);
                    changedSettings.put(configName, configurationService.getProperty(configName));
                }
                // notify the services that implement the mixin
                List<ConfigChangeListener> configChangeListeners
                    = getServicesByType(ConfigChangeListener.class);
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
                        log.error(
                            "Failure occurred while trying to notify service of config change: " + e.getMessage(),
                            e);
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
     * @param service     the service object (which will be configured)
     * @param config      the running configuration service
     */
    public static void configureService(String serviceName, Object service, ConfigurationService config) {

        // Check if the configuration has any properties whose prefix
        // corresponds to this service's name
        List<String> configKeys = config.getPropertyKeys(serviceName);
        if (configKeys != null && !configKeys.isEmpty()) {
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(service);
            for (String key : configKeys) {
                // Remove serviceName prefix from key. This is the name of the actual bean's parameter
                // This removes the first x chars, where x is length of serviceName + 1 char
                // Format of Key: [serviceName].[param]
                String param = key.substring(serviceName.length() + 1);

                try {
                    // Attempt to set this configuration on the given service's bean
                    beanWrapper.setPropertyValue(param, config.getProperty(key));
                    log.info("Set param (" + param + ") on service bean (" + serviceName + ") to: " + config
                        .getProperty(key));
                } catch (RuntimeException e) {
                    // If an error occurs, just log it
                    log.error("Unable to set param (" + param + ") on service bean (" + serviceName + ") to: " + config
                        .getProperty(key), e);
                }
            }
        }
    }

    /**
     * Initializes a service if it asks to be initialized or does nothing.
     *
     * @param service any bean
     * @throws IllegalStateException if the service init fails
     */
    public static void initService(Object service) {
        if (service instanceof InitializedService) {
            try {
                ((InitializedService) service).init();
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failure attempting to initialize service (" + service + "): " + e.getMessage(), e);
            }
        }
    }

    /**
     * Shuts down a service if it asks to be shutdown or does nothing.
     *
     * @param service any bean
     */
    public static void shutdownService(Object service) {
        if (service instanceof ShutdownService) {
            try {
                ((ShutdownService) service).shutdown();
            } catch (Exception e) {
                log.error("Failure shutting down service: {}", service, e);
            }
        }
    }

    /**
     * Build the complete list of Spring configuration paths, including
     * hard-wired paths.
     *
     * @param testMode are we testing the service manager?
     * @param configPaths paths supplied at startup.
     * @param configurationService DSpace configuration source.
     * @return
     */
    public static String[] getSpringPaths(boolean testMode, String[] configPaths,
                                          DSpaceConfigurationService configurationService) {
        List<String> pathList = new LinkedList<>();
        pathList.add(CONFIG_PATH);
        pathList.add(ADDON_RESOURCE_PATH);
        if (testMode) {
            log.warn("TEST Service Manager running in test mode:  no core beans will be started");
        } else {
            // only load the core beans when not testing the service manager
            pathList.add(CORE_RESOURCE_PATH);
        }
        if (configPaths != null) {
            pathList.addAll(Arrays.asList(configPaths));
        }
        if (testMode) {
            log.warn("TEST Spring Service Manager running in test mode, no DSpace home Spring files will be loaded");
        } else {
            //Retrieve all our spring file locations depending on the deployed module
            String[] springLoaderClassNames = configurationService.getArrayProperty("spring.springloader.modules");
            if (springLoaderClassNames != null) {
                for (String springLoaderClassName : springLoaderClassNames) {
                    try {
                        Class<SpringLoader> springLoaderClass = (Class<SpringLoader>) Class
                            .forName(springLoaderClassName.trim());
                        String[] resourcePaths = springLoaderClass.getConstructor().newInstance()
                                                                  .getResourcePaths(configurationService);
                        if (resourcePaths != null) {
                            pathList.addAll(Arrays.asList(resourcePaths));
                        }
                    } catch (ClassNotFoundException e) {
                        //Ignore this exception, if we get one this just means that this module isn't loaded
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InstantiationException | NoSuchMethodException
                            | SecurityException | InvocationTargetException e) {
                        log.error("Error while retrieving Spring resource paths for module: {}",
                                springLoaderClassName, e);
                    }
                }
            }
        }
        return pathList.toArray(new String[pathList.size()]);
    }

}
