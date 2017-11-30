/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.caching.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;


/**
 * The ehcache implementation of the Cache object.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class EhcacheCache implements Cache {

    protected Ehcache cache;
    public Ehcache getCache() {
        return cache;
    }

    protected CacheConfig cacheConfig;

    public EhcacheCache(Ehcache cache, CacheConfig cacheConfig) {
        // setup the cache
        if (cache == null) {
            throw new NullPointerException("Cache must be set and cannot be null");
        } else {
            if (cache.getStatus() != Status.STATUS_ALIVE) {
                throw new IllegalArgumentException("Cache ("+cache.getName()+") must already be initialized and alive");
            }
        }
        this.cache = cache;
        if (cacheConfig != null) {
            this.cacheConfig = cacheConfig;
        } else {
            this.cacheConfig = new CacheConfig(CacheScope.INSTANCE);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getConfig()
     */
    public CacheConfig getConfig() {
        return cacheConfig;
    }

    /* (non-Javadoc)
     * @see org.sakaiproject.caching.Cache#clear()
     */
    public void clear() {
        cache.removeAll();
        cache.clearStatistics();
    }

    /* (non-Javadoc)
     * @see org.sakaiproject.caching.Cache#exists(java.lang.String)
     */
    public boolean exists(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return cache.isKeyInCache(key);
    }

    /* (non-Javadoc)
     * @see org.sakaiproject.caching.Cache#get(java.lang.String)
     */
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        return (Serializable) getCachePayload(key, false);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getKeys()
     */
    public List<String> getKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        List<?> eKeys = cache.getKeys();
        for (Object object : eKeys) {
            if (object != null) {
                keys.add(object.toString());
            }
        }
        return keys;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#look(java.lang.String)
     */
    public Object look(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        return (Serializable) getCachePayload(key, true);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#put(java.lang.String, java.io.Serializable)
     */
    public void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        cache.put(new Element(key, value));
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Cache#getName()
     */
    public String getName() {
        if (cache != null) {
            return cache.getName();
        } else {
            return "NULL cache";
        }
    }

    /* (non-Javadoc)
     * @see org.sakaiproject.caching.Cache#remove(java.lang.String)
     */
    public boolean remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return cache.remove(key);
    }

    /* (non-Javadoc)
     * @see org.sakaiproject.caching.Cache#size()
     */
    public int size() {
        return cache.getSize();
    }

    /**
     * Retrieve a payload from the cache for this key if one can be found.
     * @param key the key for this cache element
     * @return the payload or null if none found
     */
    private Object getCachePayload(String key, boolean quiet) {
        Object payload = null;
        Element e;
        if (quiet) {
            e = cache.getQuiet(key);
        } else {
            e = cache.get(key);
        }
        if (e != null) {
            // attempt to get the serialized value first
            if (e.isSerializable()) {
                payload = e.getValue();
            } else {
                // not serializable so get the object value
                payload = e.getObjectValue();
            }
        }
        return payload;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (!(obj instanceof EhcacheCache)) {
            return false;
        } else {
            EhcacheCache castObj = (EhcacheCache) obj;
            if (null == this.getName() || null == castObj.getName()) {
                return false;
            } else {
                return (this.getName().equals(castObj.getName()));
            }
        }
    }

    @Override
    public int hashCode() {
        if (null == this.getName()) {
            return super.hashCode();
        }
        String hashStr = this.getClass().getName() + ":" + this.getName().hashCode();
        return hashStr.hashCode();
    }

    @Override
    public String toString() {
        return "EhCache:name="+getName()+":Scope="+cacheConfig.getCacheScope()+":size="+size();
    }

}
