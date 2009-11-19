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
package org.dspace.services.model;

import org.dspace.services.CachingService;


/**
 * Part of the {@link CachingService}<br/>
 * Encodes the configuration for a cache into an object
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class CacheConfig {

    /**
     * Controls the scope of each created cache
     */
    public enum CacheScope { 
        /**
         * This cache is destroyed at the end of the current request for the current thread it was created in
         */
        REQUEST,
        /**
         * This cache is destroyed when the current JVM shuts down
         */
        INSTANCE,
        /**
         * This cache is destroyed when the entire cluster goes down,
         * the cache will invalidate the same entries in other caches when it changes
         */
        CLUSTERINVALIDATED,
        /**
         * This cache is destroyed when the entire cluster goes down,
         * the cache will copy an entry over to other caches when it changes or is created
         */
        CLUSTERREPLICATED;
    };

    /**
     * Defines the lifecycle of the cache
     * @see CacheScope
     */
    private CacheScope cacheScope;
    /**
     * @return the scope of the associated cache
     */
    public CacheScope getCacheScope() {
        return cacheScope;
    }
    /**
     * Configure the cache to use the given scope
     * @param cacheScope defines the lifecycle of the cache
     */
    public CacheConfig(CacheScope cacheScope) {
        this.cacheScope = cacheScope;
    }

}
