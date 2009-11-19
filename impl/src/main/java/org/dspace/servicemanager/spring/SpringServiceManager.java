/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.dspace.servicemanager.DSpaceServiceManager;
import org.dspace.servicemanager.ServiceManagerSystem;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is the Spring implementation of the service manager
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class SpringServiceManager implements ServiceManagerSystem {

    private static Logger log = LoggerFactory.getLogger(SpringServiceManager.class);

    private ClassPathXmlApplicationContext applicationContext;
    /**
     * @return the parent core Spring {@link ApplicationContext}
     */
    public ClassPathXmlApplicationContext getApplicationContext() {
        return applicationContext;
    }
    /**
     * @return the current spring bean factory OR null if there is not one
     */
    public ListableBeanFactory getBeanFactory() {
        if (applicationContext != null) {
            return applicationContext.getBeanFactory();
        }
        return null;
    }

    private final ServiceManagerSystem parent;
    private final DSpaceConfigurationService configurationService;
    private boolean testMode = false;
    private boolean developmentMode = false;
    private String[] configPaths = null;
    /**
     * For TESTING:
     * Allows adding extra spring config paths
     * @param testMode if true then do not load the core beans
     * @param configPaths additional spring config paths within this classloader
     */
    public SpringServiceManager(ServiceManagerSystem parent, DSpaceConfigurationService configurationService, boolean testMode, boolean developmentMode, String... configPaths) {
        if (parent == null) {
            throw new IllegalArgumentException("parent SMS cannot be null");
        }
        this.parent = parent;
        if (configurationService == null) {
            throw new IllegalArgumentException("configuration service cannot be null");
        }
        this.configurationService = configurationService;
        this.testMode = testMode;
        this.developmentMode = developmentMode;
        this.configPaths = configPaths;
    }

    public static final String configPath = "spring/spring-dspace-applicationContext.xml";
    public static final String corePath = "spring/spring-dspace-core-services.xml";
    /**
     * Spring does not actually allow us to add in new singletons which have bean definitions so we
     * have to track the added singleton names ourselves manually
     */
    private Vector<String> singletonNames = new Vector<String>();

    @SuppressWarnings("unchecked")
    public <T> T getServiceByName(String name, Class<T> type) {
        T bean = null;
        // handle special case to return the core AC
        if (ApplicationContext.class.getName().equals(name) 
                && ApplicationContext.class.isAssignableFrom(type)) {
            bean = (T) getApplicationContext();
        } else {
            if (name != null) {
                // get by name and type
                try {
                    bean = (T) applicationContext.getBean(name, type);
                } catch (BeansException e) {
                    // no luck, try the fall back option
                    bean = null;
                }
            } else {
                // try making up the name based on the type
                try {
                    bean = (T) applicationContext.getBean(type.getName(), type);
                } catch (BeansException e) {
                    // no luck, try the fall back option
                    bean = null;
                }
            }
            // if still no luck then try by type only
            if (name == null 
                    && bean == null) {
                try {
                    Map<String, Object> map = applicationContext.getBeansOfType(type);
                    if (map.size() == 1) {
                        // only return the bean if there is exactly one
                        bean = (T) map.values().iterator().next();
                    }
                } catch (BeansException e) {
                    // I guess there are no beans of this type
                    bean = null;
                }
            }
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getServicesByType(Class<T> type) {
        ArrayList<T> l = new ArrayList<T>();
        Map<String, Object> beans;
        try {
            beans = applicationContext.getBeansOfType(type, true, true);
            l.addAll( (Collection<? extends T>) beans.values() );
        } catch (BeansException e) {
            throw new RuntimeException("Failed to get beans of type ("+type+"): " + e.getMessage(), e);
        }
        return l;
    }

    public void shutdown() {
        if (applicationContext != null) {
            try {
                applicationContext.close();
            } catch (Exception e) {
                // keep going anyway
                e.printStackTrace();
            }
            try {
                applicationContext.destroy();
            } catch (Exception e) {
                // keep going anyway
                e.printStackTrace();
            }
            applicationContext = null;
            log.info("Spring Service Manager Shutdown...");
        }
    }

    public void startup() {
        long startTime = System.currentTimeMillis();
        // get all spring config paths
        ArrayList<String> pathList = new ArrayList<String>();
        pathList.add(configPath);
        if (testMode) {
            log.warn("TEST Spring Service Manager running in test mode, no core beans will be started");
        } else {
            // only load the core beans when not testing the service manager
            pathList.add(corePath);
        }
        if (configPaths != null) {
            for (int i = 0; i < configPaths.length; i++) {
                pathList.add(configPaths[i]);
            }
        }
        String[] allPaths = pathList.toArray(new String[pathList.size()]);
        applicationContext = new ClassPathXmlApplicationContext(allPaths, false);
        // disable poor practices
        applicationContext.setAllowBeanDefinitionOverriding(false);
        applicationContext.setAllowCircularReferences(false);
        //applicationContext.registerShutdownHook(); // this interferes with the kernel shutdown hook
        // add the config interceptors (partially done in the xml)
        applicationContext.addBeanFactoryPostProcessor( new DSpaceBeanFactoryPostProcessor(parent, configurationService, testMode) );
        applicationContext.refresh();
        if (developmentMode) {
            log.warn("Spring Service Manager is running in developmentMode, services will be loaded on demand only");
            // TODO find a way to set this sucker to super duper lazy mode? it is currently not actually doing it
        } else {
            applicationContext.getBeanFactory().preInstantiateSingletons();
            applicationContext.getBeanFactory().freezeConfiguration();
        }
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Spring Service Manager started up in "+totalTime+" ms with "+applicationContext.getBeanDefinitionCount()+" services...");
    }

    @SuppressWarnings("unchecked")
    public <T> T registerServiceClass(String name, Class<T> type) {
        if (name == null || type == null) {
            throw new IllegalArgumentException("name and type must not be null for service registration");
        }
        T service;
        try {
            service = (T) applicationContext.getBeanFactory().autowire(type, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            registerBean(name, service);
        } catch (BeansException e) {
            throw new IllegalArgumentException("Invalid service class ("+type+") with name ("+name+") registration: " + e.getMessage(), e);
        }
        return service;
    }

    public void registerService(String name, Object service) {
        if (name == null || service == null) {
            throw new IllegalArgumentException("name and service must not be null for service registration");
        }
        try {
            applicationContext.getBeanFactory().autowireBean(service);
        } catch (BeansException e) {
            throw new IllegalArgumentException("Invalid service ("+service+") with name ("+name+") registration: " + e.getMessage(), e);
        }
        registerBean(name, service);
    }

    /**
     * This handles the common part of the 2 types of service registrations
     * @param name
     * @param service
     */
    private void registerBean(String name, Object service) {
        try {
            applicationContext.getBeanFactory().initializeBean(service, name);
            applicationContext.getBeanFactory().registerSingleton(name, service);
        } catch (BeansException e) {
            throw new IllegalArgumentException("Invalid service ("+service+") with name ("+name+") registration: " + e.getMessage(), e);
        }
        singletonNames.add(name);
    }

    public void unregisterService(String name) {
        singletonNames.remove(name);
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

    public List<String> getServicesNames() {
        ArrayList<String> beanNames = new ArrayList<String>();
        beanNames.addAll(singletonNames);
        String[] singletons = applicationContext.getBeanFactory().getSingletonNames();
        for (int i = 0; i < singletons.length; i++) {
            if (singletons[i].startsWith("org.springframework.context")) {
                continue; // skip the spring standard ones
            }
            beanNames.add(singletons[i]);
        }
        Collections.sort(beanNames);
        return beanNames;
    }

    public boolean isServiceExists(String name) {
        boolean exists = applicationContext.containsBean(name);
        return exists;
    }

    public Map<String, Object> getServices() {
        Map<String, Object> services = new HashMap<String, Object>();
        String[] singletons = applicationContext.getBeanFactory().getSingletonNames();
        for (int i = 0; i < singletons.length; i++) {
            if (singletons[i].startsWith("org.springframework.context")) {
                continue; // skip the spring standard ones
            }
            String beanName = singletons[i];
            Object service = applicationContext.getBeanFactory().getSingleton(beanName);
            if (service == null) {
                continue;
            }
            services.put(beanName, service);
        }
        return services;
    }

    public void pushConfig(Map<String, String> settings) {
        throw new UnsupportedOperationException("Not implemented for individual service manager systems");
    }

}
