/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.cache;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Removes items from the sherpaSearchByJournalISSN cache.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
public class SherpaCacheEvictService {

    // The cache that is managed by this service.
    static final String CACHE_NAME = "sherpa.searchByJournalISSN";

    @Autowired
    private CacheManager cacheManager;

    public void evictSingleCacheValue(String cacheKey) {
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evict(cacheKey);
    }

    public void evictAllCacheValues() {
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).clear();
    }

}