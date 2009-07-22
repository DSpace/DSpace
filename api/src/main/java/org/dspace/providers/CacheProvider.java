/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/providers/CacheProvider.java $
 * 
 * $Revision: 3236 $
 * 
 * $Date: 2008-10-24 09:46:39 -0700 (Fri, 24 Oct 2008) $
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
package org.dspace.providers;

import java.util.List;

import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;


/**
 * This is a provider (pluggable functionality) for DSpace<br/>
 * This allows an external system to define how caches are handled in DSpace by implementing this interface
 * and registering it with the service manager<br/>
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface CacheProvider {

    /**
     * Gets all the caches that this provider knows about
     * 
     * @return a list of all the caches which the caching service knows about
     */
    public List<Cache> getCaches();

    /**
     * Construct a {@link Cache} with the given name (must be unique) OR retrieve the one
     * that already exists with this name <br/>
     * NOTE: providers will never be asked to provide request caches (e.g. {@link CacheConfig.CacheScope#REQUEST})
     * 
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     * @param config (optional) a configuration object, the cache should adhere to the settings in it, 
     * if it is null then just use defaults
     * @return a cache which can be used to store serializable objects
     * @throws IllegalArgumentException if the cache name is already in use or the config is invalid
     */
    public Cache getCache(String cacheName, CacheConfig config);

    /**
     * Flush and destroy the cache with this name,
     * if the cache does not exist then this does nothing (should not fail if the cache does not exist)
     * @param cacheName the unique name for this cache (e.g. org.dspace.user.UserCache)
     */
    public void destroyCache(String cacheName);

    /**
     * Clears the contents of all caches managed by this provider
     */
    public void resetCaches();

}
