/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Removes items from the iiif manifests cache.
 */
@Component
public class CacheEvictService {

    // The cache that is managed by this service.
    static final String CACHE_NAME = "manifests";

    @Autowired
    CacheManager cacheManager;

    public void evictSingleCacheValue(String cacheKey) {
        cacheManager.getCache(CACHE_NAME).evict(cacheKey);
    }

    public void evictAllCacheValues() {
        cacheManager.getCache(CACHE_NAME).clear();
    }

}
