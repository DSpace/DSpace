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
package org.dspace.servicemanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.azeckoski.reflectutils.ReflectUtils;
import org.azeckoski.reflectutils.refmap.ReferenceMap;
import org.azeckoski.reflectutils.refmap.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Borrowed from EntityBus since this already will handle the mixins for something correctly and easily,
 * no need to reinvent this
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class ServiceMixinManager {

    private static Logger log = LoggerFactory.getLogger(ServiceMixinManager.class);

    /**
     * This is a map from the serviceName only to the service and also 
     * from the bikey made from the serviceName AND the implemented interfaces and superclasses to the service,
     * in other words, a service which implements 3 interfaces should have 4 entries in the map
     */
    protected ReferenceMap<String, Object> serviceNameMap = new ReferenceMap<String, Object>(ReferenceType.STRONG, ReferenceType.SOFT);

    /**
     * Looks up a service by the well known name
     * @param serviceName the well known and unique service name
     * @return the service OR null if none can be found
     */
    public Object getServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }
        // gets the main service registration
        Object service = serviceNameMap.get(serviceName);
        return service;
    }

    /**
     * @param <T>
     * @param serviceName the well known and unique service name
     * @param mixin the class type of the implemented mixin interface
     * @return the service which implements this mixin OR null if none can be found
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> T getServiceByNameAndMixin(String serviceName, Class<T> mixin) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }
        T service = null;
        if (mixin == null) {
            service = (T) getServiceByName(serviceName);
        } else {
            String bikey = getBiKey(serviceName, mixin);
            service = (T) serviceNameMap.get(bikey);
        }
        return service;
    }

    /**
     * @return the list of all known service names
     */
    public List<String> getRegisteredServiceNames() {
        Set<String> togo = new HashSet<String>();
        for (String bikey : serviceNameMap.keySet()) {
            if (bikey.indexOf('/') == -1) {
                // main service regs only
                togo.add(bikey);
            }
        }
        ArrayList<String> names = new ArrayList<String>(togo);
        Collections.sort(names);
        return names;
    }

    /**
     * @return all known registered services
     */
    public List<Object> getRegisteredServices() {
        Set<Object> togo = new HashSet<Object>();
        for (Entry<String, Object> entry : serviceNameMap.entrySet()) {
            String bikey = entry.getKey();
            if (bikey.indexOf('/') == -1) {
                // main service regs only
                togo.add( entry.getValue() );
            }
        }
        ArrayList<Object> services = new ArrayList<Object>(togo);
        Collections.sort(services, new ServiceComparator());
        return services;
    }

    /**
     * @param serviceName the well known and unique service name
     * @return the list of all known mixins for this service
     */
    public List<Class<? extends Object>> getServiceMixins(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }
        List<Class<? extends Object>> mixins = new ArrayList<Class<? extends Object>>();
        for (String bikey : serviceNameMap.keySet()) {
            String curPrefix = getServiceName(bikey);
            if (curPrefix.equals(serviceName) 
                    && ! curPrefix.equals(bikey)) {
                mixins.add( getMixin(bikey) );
            }
        }
        Collections.sort(mixins, new ClassComparator());
        return mixins;
    }

    /**
     * WARNING: this is slow so do not use it unless you really need it!
     * @return the map of all service names -> mixins that are currently registered
     */
    public Map<String, List<Class<? extends Object>>> getRegisteredServiceMixins() {
        Map<String, List<Class<? extends Object>>> m = new HashMap<String, List<Class<? extends Object>>>();
        ArrayList<String> list = new ArrayList<String>( serviceNameMap.keySet() );
        Collections.sort(list);
        for (String bikey : list) {
            String serviceName = getServiceName(bikey);
            if (! m.containsKey(serviceName)) {
                m.put(serviceName, new ArrayList<Class<? extends Object>>());
            }
            m.get(serviceName).add( getMixin(bikey) );
        }      
        return m;
    }

    /**
     * Allows retrieval of service and their names
     * @param <T>
     * @param mixin an interface which is a service mixin (technically any class)
     * @return the list of all registered services wrapped in service holders which implement this mixin interface
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> List<ServiceHolder<T>> getServiceHoldersByMixin(Class<T> mixin) {
        if (mixin == null) {
            throw new IllegalArgumentException("mixin cannot be null");
        }
        ArrayList<ServiceHolder<T>> services = new ArrayList<ServiceHolder<T>>();
        String mixinName = mixin.getName();
        for (Entry<String, Object> entry : serviceNameMap.entrySet()) {
            String name = getMixinName(entry.getKey()); // name can be null
            if (mixinName.equals(name)) {
                String serviceName = getServiceName(entry.getKey());
                services.add( new ServiceHolder<T>(serviceName, (T)entry.getValue()));
            }
        }
        Collections.sort(services, new ServiceHolderComparator());
        return services;
    }

    /**
     * @param <T>
     * @param mixin an interface which is a service mixin (technically any class)
     * @return the list of all registered services which implement this mixin interface
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> List<T> getServicesByMixin(Class<T> mixin) {
        if (mixin == null) {
            throw new IllegalArgumentException("mixin cannot be null");
        }
        ArrayList<T> services = new ArrayList<T>();
        String mixinName = mixin.getName();
        for (Entry<String, Object> entry : serviceNameMap.entrySet()) {
            String name = getMixinName(entry.getKey()); // name can be null
            if (mixinName.equals(name)) {
                services.add((T)entry.getValue());
            }
        }
        Collections.sort(services, new ServiceComparator());
        return services;
    }

    /**
     * Registers a service with the system and extracts all mixins,
     * this will manage and track the mixins and the service
     * 
     * @param serviceName the well known and unique service name
     * @param service the service itself
     * @return the list of mixin classes registered for this service
     */
    public List<Class<?>> registerService(String serviceName, Object service) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        // blast any existing service registration under this name
        unregisterServiceByName(serviceName);

        List<Class<?>> classList = new ArrayList<Class<?>>();
        // first we register the name against the service
        registerPrefixCapability(serviceName, null, service);
        // then register the name + its class against the service
        registerPrefixCapability(serviceName, service.getClass(), service);
        // then register all the mixin interfaces
        List<Class<? extends Object>> superclasses = extractMixins(service);
        int count = 0;
        for (Class<? extends Object> superclazz : superclasses) {
            registerPrefixCapability(serviceName, superclazz, service);
            classList.add(superclazz);
            count++;
            // special handling for certain services if needed
//            if (superclazz.equals(RequestAware.class)) {
//                // need to shove in the requestGetter on registration
//                ((RequestAware)service).setRequestGetter(requestGetter);
//            }
        }
        log.info("Registered service ("+service.getClass().getName()
                +") serviceName ("+serviceName+") with "+count+" mixins");
        return classList;
    }

    /**
     * Unregisters a service and throws out all the mapped mixins
     * 
     * @param serviceName the well known and unique service name
     * @return the list of mixin classes registered for this service
     */
    public List<Class<?>> unregisterServiceByName(String serviceName) {
        // NOTE: this cannot be done with only an iterator as the reference map does not support it
        List<Class<?>> classList = new ArrayList<Class<?>>();
        List<String> keys = new ArrayList<String>();
        for (Iterator<String> iterator = serviceNameMap.keySet().iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (key.startsWith(serviceName)) {
                keys.add(key);
                // get the class out of this key
                try {
                    Class<?> mixin = getMixin(key);
                    classList.add(mixin);
                } catch (Exception e) {
                    // could not get the mixin here, skip for now
                    continue;
                }
            }
        }
        // now remove the keys individually
        for (String key : keys) {
            serviceNameMap.remove(key);
        }
        return classList;
    }

    /**
     * Unregisters a single mixin from a service
     * 
     * @param serviceName the well known and unique service name
     * @param mixin an interface which is a service mixin (technically any class)
     */
    public void unregisterServiceMixin(String serviceName, Class<? extends Object> mixin) {
        if (serviceName == null || mixin == null) {
            throw new IllegalArgumentException("serviceName and mixin cannot be null");
        }
        if (Object.class.equals(mixin)) {
            throw new IllegalArgumentException(
            "Cannot separately unregister root Object mixin - use unregisterServiceByName instead");
        }
        String key = getBiKey(serviceName, mixin);
        serviceNameMap.remove(key);
        // do any cleanup that needs to be done when unregistering
        // Nothing here right now
        log.info("Unregistered service mixin ("+mixin.getName()+") for serviceName ("+serviceName+")");
    }

    /**
     * Allows for easy registration of a serviceName and mixin
     * 
     * @param serviceName
     * @param mixin
     * @param service
     * @return true if the service is newly registered, false if it was already registered
     */
    public boolean registerPrefixCapability(String serviceName,
            Class<? extends Object> mixin, Object entityProvider) {
        String key = getBiKey(serviceName, mixin);
        return serviceNameMap.put(key, entityProvider) == null;
    }

    /**
     * Clears all service registrations
     * 
     * @return the number of currently registered service names
     */
    public int clear() {
        // unregister all
        int size = size();
        serviceNameMap.clear();
        return size;
    }

    /**
     * @return the number of currently registered services
     */
    public int size() {
        int size = 0;
        for (Entry<String, Object> entry : serviceNameMap.entrySet()) {
            if (entry.getKey().indexOf('/') == -1) {
                size++;
            }
        }
        return size;
    }

    // STATICS - BIKEY methods

    protected static String getBiKey(String serviceName, Class<? extends Object> clazz) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        if (serviceName.indexOf('/') > -1) {
            throw new IllegalArgumentException("Service names cannot contain a '/' character");
        }
        String bikey = serviceName;
        if (clazz != null) {
            bikey = serviceName + "/" + clazz.getName();
        }
        return bikey;
    }

    protected static String getServiceName(String bikey) {
        if (bikey == null) {
            throw new IllegalArgumentException("bikey must not be null");
        }
        int slashpos = bikey.indexOf('/');
        String serviceName = bikey;
        if (slashpos > -1) {
            serviceName = bikey.substring(0, slashpos);
        }
        return serviceName;
    }

    protected static String getMixinName(String bikey) {
        if (bikey == null) {
            throw new IllegalArgumentException("bikey must not be null");
        }
        int slashpos = bikey.indexOf('/');
        String className = null;
        if (slashpos > -1) {
            className = bikey.substring(slashpos + 1);
        }
        return className;
    }

    protected static Class<? extends Object> getMixin(String bikey) {
        String className = getMixinName(bikey);
        return getClassByName(className);
    }


    // STATICS - OTHER

    /**
     * Attempts to get a class by name in the current classloader
     * @param className the classname (fully qualified)
     * @return the class object
     * @throws RuntimeException if the class is not found 
     */
    public static Class<? extends Object> getClassByName(String className) {
        Class<?> c = null;
        if (className != null) {
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
                try {
                    c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e1) {
                    throw new RuntimeException("Could not get Class from classname: " + className, e);
                }
            }
        }
        return (Class<? extends Object>) c;
    }

    /**
     * Get the mixins implemented by this service,
     * only gets interfaces and will not pick up superclasses <br/>
     * WARNING: this is not the fastest ever
     * 
     * @param service any object
     * @return the list of services this class implements
     */
    public static List<Class<? extends Object>> extractMixins(Object service) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        List<Class<?>> superclasses = ReflectUtils.getSuperclasses(service.getClass());
        Set<Class<? extends Object>> capabilities = new HashSet<Class<? extends Object>>();

        for (Class<?> superclazz : superclasses) {
            if (superclazz.isInterface() && Object.class.isAssignableFrom(superclazz)) {
                capabilities.add((Class<? extends Object>) superclazz);
            }
        }
        return new ArrayList<Class<? extends Object>>(capabilities);
    }

    /**
     * Compares based on the class name
     */
    public static class ClassComparator implements Comparator<Class<?>>, Serializable {
        public final static long serialVersionUID = 1l;
        public int compare(Class<?> o1, Class<?> o2) {
            if (o1 != null && o2 != null) {
                return o1.getName().compareTo(o2.getName());
            }
            return 0;
        }
    }

    /**
     * Compares based on the class name
     */
    public static class ServiceComparator implements Comparator<Object>, Serializable {
        public final static long serialVersionUID = 1l;
        public int compare(Object o1, Object o2) {
            if (o1 != null && o2 != null) {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
            return 0;
        }
    }

    /**
     * A class designed to carry services and their names for type safety
     */
    public static class ServiceHolder<T> {
        public String serviceName;
        public T service;
        /**
         * @return the service object cast to the type requested (probably not the type of the actual object)
         */
        public T getService() {
            return service;
        }
        /**
         * @return the name the service is registered under
         */
        public String getServiceName() {
            return serviceName;
        }
        public ServiceHolder(String serviceName, T service) {
            if (serviceName == null || service == null) {
                throw new IllegalArgumentException("Cannot construct service holder with null name or service");
            }
            this.serviceName = serviceName;
            this.service = service;
        }
        @Override
        public String toString() {
            return serviceName+":"+service.getClass().getName()+":"+super.toString();
        }
    }

    /**
     * Compares based on the service
     */
    public static class ServiceHolderComparator implements Comparator<ServiceHolder<?>>, Serializable {
        public final static long serialVersionUID = 1l;
        public int compare(ServiceHolder<?> o1, ServiceHolder<?> o2) {
            return o1.getServiceName().compareTo(o2.getServiceName());
        }
        
    }

}
