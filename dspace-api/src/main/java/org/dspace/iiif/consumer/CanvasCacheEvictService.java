/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.consumer;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CanvasCacheEvictService {

    // The cache that is managed by this service.
    static final String CACHE_NAME = "canvasdimensions";

    @Autowired
    CacheManager cacheManager;

    public void evictSingleCacheValue(String cacheKey) {
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent(cacheKey);
    }

}
