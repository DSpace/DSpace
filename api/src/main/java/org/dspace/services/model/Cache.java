/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/model/Cache.java $
 * 
 * $Revision: 3599 $
 * 
 * $Date: 2009-03-17 00:23:54 -0700 (Tue, 17 Mar 2009) $
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

import java.util.List;

/**
 * This is a cache which can be used to store data<br/>
 * This is an abstraction of the general concept of a cache<br/>
 * A Cache holds objects with keys with a limited lifespan and stores them based on the underlying implementation,
 * this cache interface adheres to the JSR-107 spec
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface Cache {

    /**
     * Get the unique name which identifies this cache
     * @return string
     */
    public String getName();

    /**
     * Puts an object in the cache which is identified by this key,
     * will overwrite an existing object if one is already using this key
     * @param key the key for an item in the cache
     * @param value an object (this can be a null, e.g. to cache a miss)
     * @throws IllegalArgumentException if the cache name is invalid or cacheName or key is null
     */
    public void put(String key, Object value);

    /**
     * Gets an object from the cache if it can be found (maybe be a null),
     * use the exists check to see if the object is in the cache before retrieving
     * @param key the key for an item in the cache
     * @return the cached object (may be null) OR null if the cache object cannot be found
     * @throws IllegalArgumentException if any arguments are null
     */
    public Object get(String key);

    /**
     * Provides a method for finding out what keys are currently in the cache
     * or getting all items out of the cache, the keys are returned in no
     * particular order <br/> 
     * NOTE: that this is actually quite costly in most cases and should only be used when really needed,
     * it is much better to fetch the items you actually need directly using {@link #get(String)}
     * 
     * @return the list of all keys currently in this cache
     */
    public List<String> getKeys();

    /**
     * Gets an object from the cache without causing it to be refreshed or renewed,
     * like {@link #get(String)} except that it keeps the cache from refreshing the item that was retrieved
     * @param key the key for an item in the cache
     * @return the cached object (may be null) OR null if the cache object cannot be found
     * @throws IllegalArgumentException if any arguments are null
     * @see Cache#get(String)
     */
    public Object look(String key);

    /**
     * Removes an object from the cache if it exists or does nothing if it does not
     * @param key the key for an item in the cache
     * @return true if the object was removed or false if it could not be found in the cache
     * @throws IllegalArgumentException if any arguments are null
     */
    public boolean remove(String key);

    /**
     * Check if a key exists in the cache and return true if it does
     * @param key the key for an item in the cache
     * @return true if the object was removed or false if it could not be found in the cache
     * @throws IllegalArgumentException if any arguments are null
     */
    public boolean exists(String key);

    /**
     * @return the count of the number of active items in the cache,
     * does not include expired items
     */
    public int size();

    /**
     * Clear out all cached items from this cache
     */
    public void clear();

    /**
     * Returns a readable object which has the configuration used by this cache in it
     * @return the object indicating the configuration of this cache
     */
    public CacheConfig getConfig();

}
