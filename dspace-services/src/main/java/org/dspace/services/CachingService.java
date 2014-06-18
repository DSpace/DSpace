/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.List;

import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;

/**
 * A service to manage creation and retrieval of caches.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface CachingService {

    /**
     * This is the cache key used to stored requests in a request cache,
     * typically handled by a servlet filter but can be handled by 
     * anything.
     * This is here to ensure we use the right name.
     */
    public static final String REQUEST_CACHE = "dsRequestCache";

    /**
     * Gets all the caches that the service knows about.
     * This will include caches of all scopes but only includes request
     * caches for the current thread.
     * 
     * @return a list of all the caches which the caching service knows about
     */
    public List<Cache> getCaches();

    /**
     * Construct a Cache with the given name OR retrieve the one that
     * already exists with this name.  Often the name is the fully
     * qualified classpath of the API for the service that is being
     * cached, or of the class if there is no API.
     * This will operate on system defaults (probably a distributed
     * cache without replication) OR it will use the defaults which are 
     * configured for this cacheName (part of the underlying
     * implementation) if the cacheConfig is null.
     * <p>
     * This can only retrieve request caches for the current request.
     * <p>
     * If the cache already exists then the cacheConfig is ignored.
     * 
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     * @param cacheConfig defines the configuration for this cache
     * @return a cache which can be used to store objects
     */
    public Cache getCache(String cacheName, CacheConfig cacheConfig);

    /**
     * Flushes and destroys the cache with this name.
     * Generally there is no reason to call this.
     * 
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     */
    public void destroyCache(String cacheName);

    /**
     * Get a status report of cache usage which is suitable for log or 
     * screen output.
     * 
     * @param cacheName (optional) the unique name for this cache (e.g. org.dspace.user.UserCache) 
     * OR null for status of all caches
     * @return a string representing the current status of the specified cache or all caches
     */
    public String getStatus(String cacheName);

    /**
     * Clears all caches.
     * Generally there is no reason to call this.
     * 
     * @throws SecurityException if the current user does not have super user permissions
     */
    public void resetCaches();

    /**
     * Unbinds all request caches.  Destroys the caches completely.
     * You should not call this unless you know what you are doing;
     * it is handled automatically by the system.
     */
    public void unbindRequestCaches();

}
