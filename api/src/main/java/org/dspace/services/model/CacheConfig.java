/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/model/CacheConfig.java $
 * 
 * $Revision: 3231 $
 * 
 * $Date: 2008-10-24 02:10:30 -0700 (Fri, 24 Oct 2008) $
 *
 * Copyright (c) 2008, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its 
 * contributors may be used to endorse or promote products derived from 
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
