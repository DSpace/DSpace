/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;

/**
 * Encodes the configuration for a cache into an object.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class CacheConfig {

    /**
     * Controls the scope of each created cache.
     */
    public enum CacheScope { 
        /**
         * This cache is destroyed at the end of the current request for 
         * the current thread it was created in.
         */
        REQUEST,
        /**
         * This cache is destroyed when the current JVM shuts down.
         */
        INSTANCE,
        /**
         * This cache is destroyed when the entire cluster goes down.
         * The cache will invalidate the same entries in other caches
         * when it changes.
         */
        CLUSTERINVALIDATED,
        /**
         * This cache is destroyed when the entire cluster goes down.
         * The cache will copy an entry over to other caches when it
         * changes or is created.
         */
        CLUSTERREPLICATED;
    };

    /**
     * Defines the lifecycle of the cache.
     * @see CacheScope
     */
    private CacheScope cacheScope;
    /**
     * @return the scope of the associated cache
     */
    public final CacheScope getCacheScope() {
        return cacheScope;
    }
    /**
     * Configure the cache to use the given scope.
     * @param cacheScope defines the lifecycle of the cache
     */
    public CacheConfig(CacheScope cacheScope) {
        this.cacheScope = cacheScope;
    }

}
