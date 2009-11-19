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
package org.dspace.services.caching;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.management.ManagementService;

import org.dspace.kernel.ServiceManager;
import org.dspace.kernel.mixins.ConfigChangeListener;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ServiceChangeListener;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.providers.CacheProvider;
import org.dspace.services.CachingService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.caching.model.EhcacheCache;
import org.dspace.services.caching.model.MapCache;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.utils.servicemanager.ProviderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * This is the core caching service which is available for anyone who is writing code for DSpace to use
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class CachingServiceImpl implements CachingService, InitializedService, ShutdownService, ConfigChangeListener, ServiceChangeListener {

    private static Logger log = LoggerFactory.getLogger(CachingServiceImpl.class);

    /**
     * This is the event key for a full cache reset
     */
    protected static final String EVENT_RESET = "caching.reset";
    /**
     * The default config location
     */
    protected static final String DEFAULT_CONFIG = "org/dspace/services/caching/ehcache-config.xml";

    /**
     * all the non-thread caches that we know about,
     * mostly used for tracking purposes
     */
    private Map<String, EhcacheCache> cacheRecord = new ConcurrentHashMap<String, EhcacheCache>();
    /**
     * All the request caches, this is bound to the thread,
     * the initial value of this TL is set automatically when it is created
     */
    private ThreadLocal<Map<String, MapCache>> requestMap = new ThreadLocalMap();
    /**
     * @return the current request map which is bound to the current thread
     */
    protected Map<String, MapCache> getRequestMap() {
        return requestMap.get();
    }

    /**
     * Unbinds all request caches, destroys the caches completely
     */
    public void unbindRequestCaches() {
        // not sure if I really need to clear these first, it should be sufficient to just wipe the request map -AZ
        for (MapCache mc : requestMap.get().values()) {
            mc.clear();
        }
        requestMap.remove(); // clear the TL entirely
    }

    private ConfigurationService configurationService;
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private ServiceManager serviceManager;
    @Autowired
    @Required
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    /** The underlying cache manager; injected */
    protected net.sf.ehcache.CacheManager cacheManager;
    @Autowired
    @Required
    public void setCacheManager(net.sf.ehcache.CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    public net.sf.ehcache.CacheManager getCacheManager() {
        return cacheManager;
    }

    public boolean useClustering = false;
    public boolean useDiskStore = true;
    public int maxElementsInMemory = 2000;
    public int timeToLiveSecs = 3600;
    public int timeToIdleSecs = 600;

    /**
     * Reloads the config settings from the configuration service
     */
    protected void reloadConfig() {
        useClustering = configurationService.getPropertyAsType(knownConfigNames[0], boolean.class);
        useDiskStore = configurationService.getPropertyAsType(knownConfigNames[1], boolean.class);
        maxElementsInMemory = configurationService.getPropertyAsType(knownConfigNames[2], int.class);
        timeToLiveSecs = configurationService.getPropertyAsType(knownConfigNames[3], int.class);
        timeToIdleSecs = configurationService.getPropertyAsType(knownConfigNames[4], int.class);
    }

    /**
     * WARNING: Do not change the order of these! <br/>
     * If you do, you have to fix the {@link #reloadConfig()} method -AZ
     */
    private String[] knownConfigNames = {
            "caching.use.clustering", // bool - whether to use clustering
            "caching.default.use.disk.store", // whether to use the disk store
            "caching.default.max.elements", // the maximum number of elements in memory, before they are evicted
            "caching.default.time.to.live.secs", // the default amount of time to live for an element from its creation date
            "caching.default.time.to.idle.secs", // the default amount of time to live for an element from its last accessed or modified date
    };
    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ConfigChangeListener#notifyForConfigNames()
     */
    public String[] notifyForConfigNames() {
        return knownConfigNames == null ? null : knownConfigNames.clone();
    }
    
    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ConfigChangeListener#configurationChanged(java.util.List, java.util.Map)
     */
    public void configurationChanged(List<String> changedSettingNames, Map<String, String> changedSettings) {
        reloadConfig();
    }

    /**
     * This will make it easier to handle a provider which might go away because the classloader is gone
     */
    private ProviderHolder<CacheProvider> provider = new ProviderHolder<CacheProvider>();
    public CacheProvider getCacheProvider() {
        return provider.getProvider();
    }
    private void reloadProvider() {
        boolean current = (getCacheProvider() != null);
        CacheProvider cacheProvider = serviceManager.getServiceByName(CacheProvider.class.getName(), CacheProvider.class);
        provider.setProvider(cacheProvider);
        if (cacheProvider != null) {
            log.info("Cache Provider loaded: " + cacheProvider.getClass().getName());
        } else {
            if (current) {
                log.info("Cache Provider unloaded");
            }
        }
    }


    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#notifyForTypes()
     */
    public Class<?>[] notifyForTypes() {
        return new Class<?>[] { CacheProvider.class };
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#serviceRegistered(java.lang.String, java.lang.Object, java.util.List)
     */
    public void serviceRegistered(String serviceName, Object service, List<Class<?>> implementedTypes) {
        provider.setProvider((CacheProvider) service);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#serviceUnregistered(java.lang.String, java.lang.Object)
     */
    public void serviceUnregistered(String serviceName, Object service) {
        provider.setProvider(null);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.InitializedService#init()
     */
    public void init() {
        log.info("init()");
        // get settings
        reloadConfig();
        // make sure we have a cache manager
        if (cacheManager == null) {
            // not injected so we need to create one
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream is = cl.getResourceAsStream(DEFAULT_CONFIG);
            if (is == null) {
                throw new IllegalStateException("Could not init the cache manager, no config file found as a resource in the classloader: " + DEFAULT_CONFIG);
            }
            cacheManager = new CacheManager(is);
        }
        // register the cache manager as an MBean
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ManagementService.registerMBeans(cacheManager, mbs, true, true, false, false);
        // get all caches out of the cachemanager and load them into the cache list
        List<Ehcache> ehcaches = getAllEhCaches(false);
        for (Ehcache ehcache : ehcaches) {
            EhcacheCache cache = new EhcacheCache(ehcache, null);
            cacheRecord.put(cache.getName(), cache);
        }
        // load provider
        reloadProvider();
        log.info("Caching service initialized:\n" + getStatus(null));
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ShutdownService#shutdown()
     */
    public void shutdown() {
        log.info("destroy()");
        // for some reason this causes lots of errors so not using it for now -AZ
        //ehCacheManagementService.dispose();
        try {
            cacheRecord.clear();
        } catch (RuntimeException e) {
            // whatever
        }
        try {
            getRequestMap().clear();
        } catch (RuntimeException e) {
            // whatever
        }
        try {
            cacheManager.removalAll();
        } catch (RuntimeException e) {
            // whatever
        }
        cacheManager.shutdown();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#destroyCache(java.lang.String)
     */
    public void destroyCache(String cacheName) {
        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("cacheName cannot be null or empty string");
        }

        // handle provider first
        if (getCacheProvider() != null) {
            try {
                getCacheProvider().destroyCache(cacheName);
            } catch (Exception e) {
                log.warn("Failure in provider ("+getCacheProvider()+"): " + e.getMessage());
            }
        }

        EhcacheCache cache = cacheRecord.get(cacheName);
        if (cache != null) {
            cacheManager.removeCache(cacheName);
            cacheRecord.remove(cacheName);
        } else {
            getRequestMap().remove(cacheName);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getCache(java.lang.String, org.dspace.services.model.CacheConfig)
     */
    public Cache getCache(String cacheName, CacheConfig cacheConfig) {
        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("cacheName cannot be null or empty string");
        }

        // find the cache in the records if possible
        Cache cache = this.cacheRecord.get(cacheName);
        if (cache == null) {
            cache = this.getRequestMap().get(cacheName);
        }

        // handle provider
        if (getCacheProvider() != null) {
            if (cache == null 
                    && cacheConfig != null 
                    && ! CacheScope.REQUEST.equals(cacheConfig.getCacheScope()) ) {
                try {
                    cache = getCacheProvider().getCache(cacheName, cacheConfig);
                } catch (Exception e) {
                    log.warn("Failure in provider ("+getCacheProvider()+"): " + e.getMessage());
                }
            }
        }

        // no cache found so make one
        if (cache == null) {
            // create the cache type based on the cache config
            if (cacheConfig != null 
                    && CacheScope.REQUEST.equals(cacheConfig.getCacheScope()) ) {
                cache = instantiateMapCache(cacheName, cacheConfig);
            } else {
                cache = instantiateEhCache(cacheName, cacheConfig);
            }
        }
        return cache;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getCaches()
     */
    public List<Cache> getCaches() {
        List<Cache> caches = new ArrayList<Cache>(this.cacheRecord.values());
        if (getCacheProvider() != null) {
            try {
                caches.addAll( getCacheProvider().getCaches() );
            } catch (Exception e) {
                log.warn("Failure in provider ("+getCacheProvider()+"): " + e.getMessage());
            }
        }
        caches.addAll(this.getRequestMap().values());
        Collections.sort(caches, new NameComparator());
        return caches;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getStatus(java.lang.String)
     */
    public String getStatus(String cacheName) {
        final StringBuilder sb = new StringBuilder();

        if (cacheName == null || "".equals(cacheName)) {
            // add in overall memory stats
            sb.append("** Memory report\n");
            sb.append(" freeMemory: " + Runtime.getRuntime().freeMemory());
            sb.append("\n");
            sb.append(" totalMemory: " + Runtime.getRuntime().totalMemory());
            sb.append("\n");
            sb.append(" maxMemory: " + Runtime.getRuntime().maxMemory());
            sb.append("\n");

            // caches summary report
            List<Cache> allCaches = getCaches();
            sb.append("\n** Full report of all known caches ("+allCaches.size()+"):\n");
            for (Cache cache : allCaches) {
                sb.append(" * ");
                sb.append(cache.toString());
                sb.append("\n");
                if (cache instanceof EhcacheCache) {
                    Ehcache ehcache = ((EhcacheCache)cache).getCache();
                    sb.append(generateCacheStats(ehcache));
                    sb.append("\n");
                }
            }
        } else {
            // report for a single cache
            sb.append("\n** Report for cache ("+cacheName+"):\n");
            Cache cache = this.cacheRecord.get(cacheName);
            if (cache == null) {
                cache = this.getRequestMap().get(cacheName);
            }
            if (cache == null) {
                sb.append(" * Could not find cache by this name: " + cacheName);
            } else {
                sb.append(" * ");
                sb.append(cache.toString());
                sb.append("\n");
                if (cache instanceof EhcacheCache) {
                    Ehcache ehcache = ((EhcacheCache)cache).getCache();
                    sb.append(generateCacheStats(ehcache));
                    sb.append("\n");
                }
            }
        }

        final String rv = sb.toString();
        return rv;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#resetCaches()
     */
    public void resetCaches() {
        log.debug("resetCaches()");

        List<Cache> allCaches = getCaches();
        for (Cache cache : allCaches) {
            cache.clear();
        }

        if (getCacheProvider() != null) {
            try {
                getCacheProvider().resetCaches();
            } catch (Exception e) {
                log.warn("Failure in provider ("+getCacheProvider()+"): " + e.getMessage());
            }
        }

        System.runFinalization(); // force the JVM to try to clean up any remaining objects
        // DO NOT CALL System.gc() here or I will have you shot -AZ

        log.info("doReset(): Memory Recovery to: " + Runtime.getRuntime().freeMemory());
    }

    /**
     * Return all caches from the CacheManager
     * 
     * @param sorted if true then sort by name
     * @return the list of all known ehcaches
     */
    protected List<Ehcache> getAllEhCaches(boolean sorted) {
        log.debug("getAllCaches()");

        final String[] cacheNames = cacheManager.getCacheNames();
        if (sorted) {
            Arrays.sort(cacheNames);
        }
        final List<Ehcache> caches = new ArrayList<Ehcache>(cacheNames.length);
        for (String cacheName : cacheNames) {
            caches.add(cacheManager.getEhcache(cacheName));
        }
        return caches;
    }

    /**
     * Create an EhcacheCache (and the associated EhCache) using the supplied name (with default settings) 
     * or get the cache out of spring or the current configured cache <br/>
     * This expects that the cacheRecord has already been checked and will not check it again <br/>
     * Will proceed in this order:
     * 1) Attempt to load a bean with the name of the cache
     * 2) Attempt to load cache from caching system
     * 3) Create a new cache by this name
     * 4) Put the cache in the cache record
     * 
     * @param cacheName the name of the cache
     * @param cacheConfig the config for this cache
     * @return a cache instance
     */
    protected EhcacheCache instantiateEhCache(String cacheName, CacheConfig cacheConfig) {
        if (log.isDebugEnabled())
            log.debug("instantiateEhCache(String " + cacheName + ")");

        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("String cacheName must not be null or empty!");
        }

        // try to locate a named cache in the service manager
        Ehcache ehcache = serviceManager.getServiceByName(cacheName, Ehcache.class);

        // try to locate the cache in the cacheManager by name
        if (ehcache == null) {
            // if this cache name is created or already in use then we just get it
            if (!cacheManager.cacheExists(cacheName)) {
                // did not find the cache
                if (cacheConfig == null) {
                    cacheManager.addCache(cacheName); // create a new cache using ehcache defaults
                } else {
                    if (useClustering) {
                        // TODO
                        throw new UnsupportedOperationException("Still need to do this");
                    } else {
                        cacheManager.addCache(cacheName); // create a new cache using ehcache defaults
                    }
                }
                log.info("Created new Cache (from default settings): " + cacheName);
            }
            ehcache = cacheManager.getEhcache(cacheName);
        }
        // wrap the ehcache in the cache impl
        EhcacheCache cache = new EhcacheCache(ehcache, cacheConfig);
        cacheRecord.put(cacheName, cache);
        return cache;
    }

    /**
     * Create a thread map cache using the supplied name with supplied settings <br/>
     * This expects that the cacheRecord has already been checked and will not check it again <br/>
     * It also places the cache into the request map
     * 
     * @param cacheName the name of the cache
     * @param cacheConfig the config for this cache
     * @return a cache instance
     */
    protected MapCache instantiateMapCache(String cacheName, CacheConfig cacheConfig) {
        if (log.isDebugEnabled())
            log.debug("instantiateMapCache(String " + cacheName + ")");

        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("String cacheName must not be null or empty!");
        }

        // check for existing cache
        MapCache cache = null;
        CacheScope scope = CacheScope.REQUEST;
        if (cacheConfig != null) {
            scope = cacheConfig.getCacheScope();
        }
        if (CacheScope.REQUEST.equals(scope)) {
            cache = getRequestMap().get(cacheName);
        }

        if (cache == null) {
            cache = new MapCache(cacheName, cacheConfig);
            // place cache into the right TL
            if (CacheScope.REQUEST.equals(scope)) {
                getRequestMap().put(cacheName, cache);
            }
        }
        return cache;
    }

    /**
     * Generate some stats for this cache,
     * note that this is not cheap so do not use it very often
     * @param cache an Ehcache
     * @return the stats of this cache as a string
     */
    protected static String generateCacheStats(Ehcache cache) {
        StringBuilder sb = new StringBuilder();
        sb.append(cache.getName() + ":");
        // this will make this costly but it is important to get accurate settings
        cache.setStatisticsAccuracy(Statistics.STATISTICS_ACCURACY_GUARANTEED);
        Statistics stats = cache.getStatistics();
        final long memSize = cache.getMemoryStoreSize();
        final long diskSize = cache.getDiskStoreSize();
        final long size = memSize + diskSize;
        final long hits = stats.getCacheHits();
        final long misses = stats.getCacheMisses();
        final String hitPercentage = ((hits+misses) > 0) ? ((100l * hits) / (hits + misses)) + "%" : "N/A";
        final String missPercentage = ((hits+misses) > 0) ? ((100l * misses) / (hits + misses)) + "%" : "N/A";
        sb.append("  Size: " + size + " [memory:" + memSize + ", disk:" + diskSize + "]");
        sb.append(",  Hits: " + hits + " [memory:" + stats.getInMemoryHits() + 
                ", disk:" + stats.getOnDiskHits() + "] (" + hitPercentage + ")");
        sb.append(",  Misses: " + misses + " (" + missPercentage + ")");
        return sb.toString();
    }

    public static class NameComparator implements Comparator<Cache>, Serializable {
        public final static long serialVersionUID = 1l;
        public int compare(Cache o1, Cache o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}
