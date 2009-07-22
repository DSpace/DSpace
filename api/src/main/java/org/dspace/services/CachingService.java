/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/CachingService.java $
 * 
 * $Revision: 3253 $
 * 
 * $Date: 2008-10-30 05:02:18 -0700 (Thu, 30 Oct 2008) $
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

package org.dspace.services;

import java.util.List;

import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;

/**
 * A service to manage creation and retrieval of caches
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface CachingService {

    /**
     * This is the cache key used to stored requests in a request cache,
     * typically handled by a servlet filter but can be handled by anything,
     * this is here to ensure we use the right name
     */
    public static final String REQUEST_CACHE = "dsRequestCache";
    /**
     * This is the key in the request cache which holds the current http request (if there is one)
     */
    public static final String HTTP_REQUEST_KEY = "httpRequest";
    /**
     * This is the key in the request cache which holds the current http response (if there is one)
     */
    public static final String HTTP_RESPONSE_KEY = "httpResponse";
    /**
     * This is the key in the request cache which holds the current request
     */
    public static final String REQUEST_KEY = "request";
    /**
     * This is the key in the request cache which holds the current response
     */
    public static final String RESPONSE_KEY = "response";
    /**
     * This is the key in the request cache which holds the current locale
     */
    public static final String LOCALE_KEY = "locale";
    /**
     * This is the key in the request cache which holds the current session ID
     */
    public static final String SESSION_ID_KEY = "session";
    /**
     * This is the key in the request cache which holds the current request ID
     */
    public static final String REQUEST_ID_KEY = "requestId";

    /**
     * Gets all the caches that the service knows about,
     * this will include caches of all scopes but only includes request caches for the current thread
     * 
     * @return a list of all the caches which the caching service knows about
     */
    public List<Cache> getCaches();

    /**
     * Construct a Cache with the given name (often this is the fully qualified classpath of the api 
     * for the service that is being cached or the class if there is no api) OR retrieve the one
     * that already exists with this name,
     * this will operate on system defaults (probably a distributed cache without replication) OR
     * it will use the defaults which are configured for this cacheName (part of the underlying implementation)
     * if the cacheConfig is null <br/>
     * This can only retrieve request caches for the current request <br/>
     * If the cache already exists then the cacheConfig is ignored <br/>
     * 
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     * @param cacheConfig defines the configuration for this cache
     * @return a cache which can be used to store objects
     */
    public Cache getCache(String cacheName, CacheConfig cacheConfig);

    /**
     * Flushes and destroys the cache with this name,
     * generally there is no reason to call this
     * 
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     */
    public void destroyCache(String cacheName);

    /**
     * Get a status report of cache usage which is suitable for log or screen output
     * 
     * @param cacheName (optional) the unique name for this cache (e.g. org.dspace.user.UserCache) 
     * OR null for status of all caches
     * @return a string representing the current status of the specified cache or all caches
     */
    public String getStatus(String cacheName);

    /**
     * Clears all caches,
     * generally there is no reason to call this
     * 
     * @throws SecurityException if the current user does not have super user permissions
     */
    public void resetCaches();

    /**
     * Unbinds all request caches, destroys the caches completely,
     * you should not call this unless you know what you are doing, 
     * it is handled automatically by the system
     */
    public void unbindRequestCaches();

}
