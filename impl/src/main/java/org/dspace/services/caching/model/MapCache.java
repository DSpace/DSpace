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
package org.dspace.services.caching.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;


/**
 * This is a simple cache that just uses a map to store the cache values,
 * used for the request and thread caches
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class MapCache implements Cache {

    private HashMap<String, Object> cache;
    public HashMap<String, Object> getCache() {
        return cache;
    }

    protected String name;
    protected CacheConfig cacheConfig;

    public MapCache(String name, CacheConfig cacheConfig) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
        this.cache = new HashMap<String, Object>();
        if (cacheConfig != null) {
            this.cacheConfig = cacheConfig;
        } else {
            this.cacheConfig = new CacheConfig(CacheScope.REQUEST);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#clear()
     */
    public void clear() {
        this.cache.clear();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#exists(java.lang.String)
     */
    public boolean exists(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return this.cache.containsKey(key);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#get(java.lang.String)
     */
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return this.cache.get(key);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getKeys()
     */
    public List<String> getKeys() {
        ArrayList<String> keys = new ArrayList<String>(this.cache.keySet());
        return keys;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getConfig()
     */
    public CacheConfig getConfig() {
        return this.cacheConfig;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getName()
     */
    public String getName() {
        return this.name;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#look(java.lang.String)
     */
    public Object look(String key) {
        return get(key);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#put(java.lang.String, java.io.Serializable)
     */
    public void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        this.cache.put(key, value);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#remove(java.lang.String)
     */
    public boolean remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return this.cache.remove(key) != null;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#size()
     */
    public int size() {
        return this.cache.size();
    }

    @Override
    public String toString() {
        return "MapCache:name="+getName()+":Scope="+cacheConfig.getCacheScope()+":size="+size();
    }

}
