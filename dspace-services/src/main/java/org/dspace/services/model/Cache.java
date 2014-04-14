/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;

import java.util.List;

/**
 * This is a cache which can be used to store data.
 * <p>
 * This is an abstraction of the general concept of a cache.
 * <p>
 * A Cache holds objects with keys with a limited lifespan and stores 
 * them based on the underlying implementation.  This cache interface 
 * adheres to the JSR-107 spec.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface Cache {

    /**
     * Get the unique name which identifies this cache.
     *
     * @return string
     */
    public String getName();

    /**
     * Puts an object in the cache which is identified by this key.
     * Will overwrite an existing object if one is already using this key.
     *
     * @param key the key for an item in the cache
     * @param value an object (this can be a null, e.g. to cache a miss)
     * @throws IllegalArgumentException if the cache name is invalid or cacheName or key is null
     */
    public void put(String key, Object value);

    /**
     * Gets an object from the cache if it can be found (maybe be a null).
     * Use the exists check to see if the object is in the cache before
     * retrieving.
     *
     * @param key the key for an item in the cache
     * @return the cached object (may be null) OR null if the cache object cannot be found
     * @throws IllegalArgumentException if any arguments are null
     */
    public Object get(String key);

    /**
     * Provides a method for finding out what keys are currently in the cache
     * or getting all items out of the cache.  The keys are returned in
     * no particular order.
     * <p>
     * NOTE: that this is actually quite costly in most cases and should 
     * only be used when really needed.  It is much better to fetch the
     * items you actually need directly using {@link #get(String)}.
     * 
     * @return the list of all keys currently in this cache
     */
    public List<String> getKeys();

    /**
     * Gets an object from the cache without causing it to be refreshed 
     * or renewed.  Like {@link #get(String)} except that it keeps the
     * cache from refreshing the item that was retrieved.
     *
     * @param key the key for an item in the cache
     * @return the cached object (may be null) OR null if the cache object cannot be found
     * @throws IllegalArgumentException if any arguments are null
     * @see Cache#get(String)
     */
    public Object look(String key);

    /**
     * Removes an object from the cache if it exists or does nothing if 
     * it does not.
     *
     * @param key the key for an item in the cache
     * @return true if the object was removed or false if it could not be found in the cache
     * @throws IllegalArgumentException if any arguments are null
     */
    public boolean remove(String key);

    /**
     * Check if a key exists in the cache.
     *
     * @param key the key for an item in the cache
     * @return true if the object was removed or false if it could not be found in the cache
     * @throws IllegalArgumentException if any arguments are null
     */
    public boolean exists(String key);

    /**
     * How many items does this cache hold?
     *
     * @return the count of the number of active items in the cache.
     * Does not include expired items.
     */
    public int size();

    /**
     * Clear out all cached items from this cache.
     */
    public void clear();

    /**
     * Returns a readable object which has the configuration used by 
     * this cache in it.
     *
     * @return the object indicating the configuration of this cache
     */
    public CacheConfig getConfig();

}
