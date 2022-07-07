/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.cache;

import java.util.Objects;
import java.util.Set;

import org.dspace.app.sherpa.submit.SHERPASubmitService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.cache.CacheManager;

/**
 * This service is responsible to deal with the SherpaService cache.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SherpaCacheEvictService {

    // The cache that is managed by this service.
    static final String CACHE_NAME = "sherpa.searchByJournalISSN";

    private CacheManager cacheManager;

    private SHERPASubmitService sherpaSubmitService;

    /**
     * Remove immediately from the cache all the response that are related to a specific item
     * extracting the ISSNs from the item
     * 
     * @param context The DSpace context
     * @param item    an Item
     */
    public void evictCacheValues(Context context, Item item) {
        Set<String> ISSNs = sherpaSubmitService.getISSNs(context, item);
        for (String issn : ISSNs) {
            Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evictIfPresent(issn);
        }
    }

    /**
     * Invalidate immediately the Sherpa cache
     */
    public void evictAllCacheValues() {
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).invalidate();
    }

    /**
     * Set the reference to the cacheManager
     * 
     * @param cacheManager
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Set the reference to the SherpaSubmitService
     * 
     * @param sherpaSubmitService
     */
    public void setSherpaSubmitService(SHERPASubmitService sherpaSubmitService) {
        this.sherpaSubmitService = sherpaSubmitService;
    }

}