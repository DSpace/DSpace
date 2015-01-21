/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.caching;

import static org.junit.Assert.*;

import java.util.List;

import org.dspace.services.RequestService;
import org.dspace.services.caching.model.EhcacheCache;
import org.dspace.services.caching.model.MapCache;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Testing the caching service
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class CachingServiceTest extends DSpaceAbstractKernelTest {

    private CachingServiceImpl cachingService;
    private RequestService requestService;

    @Before
    public void init() {
        cachingService = getService(CachingServiceImpl.class);
        requestService = getService(RequestService.class);
        assertNotNull(cachingService);
        assertNotNull(requestService);
    }

    @After
    public void tearDown() {
        cachingService = null;
        requestService = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#reloadConfig()}.
     */
    @Test
    public void testReloadConfig() {
        // just make sure no failure
        cachingService.reloadConfig();
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#notifyForConfigNames()}.
     */
    @Test
    public void testNotifyForConfigNames() {
        assertNotNull(cachingService.notifyForConfigNames());
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#instantiateEhCache(java.lang.String, org.dspace.services.model.CacheConfig)}.
     */
    @Test
    public void testInstantiateEhCache() {
        EhcacheCache cache = cachingService.instantiateEhCache("aaronz-eh", null); // make default ehcache
        assertNotNull(cache);
        assertEquals("aaronz-eh", cache.getName());
        assertNotNull(cache.getCache());
        assertEquals(0, cache.size());
        assertNotNull(cache.getConfig());
        assertEquals(cache.getConfig().getCacheScope(), CacheScope.INSTANCE);

        EhcacheCache cache2 = cachingService.instantiateEhCache("aaronz-eh", null);
        assertNotNull(cache2);
        assertEquals(cache2, cache);
        
        //trash the references
        cache = cache2 = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#instantiateMapCache(java.lang.String, org.dspace.services.model.CacheConfig)}.
     */
    @Test
    public void testInstantiateMapCache() {
        requestService.startRequest();

        MapCache cache = cachingService.instantiateMapCache("aaronz-map", null);
        assertNotNull(cache);
        assertEquals("aaronz-map", cache.getName());
        assertNotNull(cache.getCache());
        assertEquals(0, cache.size());
        assertNotNull(cache.getConfig());
        assertEquals(cache.getConfig().getCacheScope(), CacheScope.REQUEST);

        MapCache cache2 = cachingService.instantiateMapCache("aaronz-map", null);
        assertNotNull(cache2);
        assertEquals(cache2, cache);

        requestService.endRequest(null);
        
        //trash the references
        cache = cache2 = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#getCache(java.lang.String, org.dspace.services.model.CacheConfig)}.
     */
    @Test
    public void testGetCache() {
        // test getting ehcache from the config
        Cache cache = cachingService.getCache("org.dspace.caching.MemOnly", null);
        assertNotNull(cache);
        assertEquals("org.dspace.caching.MemOnly", cache.getName());

        // test getting ehcache from bean
        Cache sampleCache = cachingService.getCache("org.sakaiproject.caching.test.SampleCache", null);
        assertNotNull(sampleCache);
        assertEquals("org.sakaiproject.caching.test.SampleCache", sampleCache.getName());

        // test making new caches
        Cache c1 = cachingService.getCache("org.dspace.aztest", null);
        assertNotNull(c1);
        assertEquals("org.dspace.aztest", c1.getName());
        assertEquals(CacheScope.INSTANCE, c1.getConfig().getCacheScope());
        assertTrue(c1 instanceof EhcacheCache);

        requestService.startRequest();

        Cache rc1 = cachingService.getCache("org.dspace.request.cache1", new CacheConfig(CacheScope.REQUEST));
        assertNotNull(rc1);
        assertEquals("org.dspace.request.cache1", rc1.getName());
        assertEquals(CacheScope.REQUEST, rc1.getConfig().getCacheScope());
        assertTrue(rc1 instanceof MapCache);

        requestService.endRequest(null);

        // try getting the same one twice
        Cache c2 = cachingService.getCache("org.dspace.aztest", null);
        assertNotNull(c2);
        assertEquals(c1, c2);
        
        //trash the references
        cache = sampleCache = c1 = rc1 = c2 = null;

    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#getCaches()}.
     */
    @Test
    public void testGetCaches() {
        List<Cache> caches = cachingService.getCaches();
        assertNotNull(caches);
        int curSize = caches.size();
        assertTrue(curSize > 0);

        Cache memCache = cachingService.getCache("org.dspace.caching.MemOnly", null);
        assertNotNull(memCache);
        assertTrue(caches.contains(memCache));

        // This should create a new cache (as cache name is unique)
        Cache c1 = cachingService.getCache("org.dspace.timtest.newcache", null);
        assertNotNull(c1);

        // Test that new cache was created and total caches increases by one 
        caches = cachingService.getCaches();
        assertNotNull(caches);
        assertEquals(curSize+1, caches.size());
        assertTrue(caches.contains(c1));
        
        //trash the references
        memCache = c1 = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#getStatus(java.lang.String)}.
     */
    @Test
    public void testGetStatus() {
        String status = cachingService.getStatus(null);
        assertNotNull(status);

        // make sure invalid cache is not a failure
        status = cachingService.getStatus("XXXXXXXXX");
        assertNotNull(status);
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#resetCaches()}.
     */
    @Test
    public void testResetCaches() {
        cachingService.resetCaches();

        // now add new cache
        Cache c1 = cachingService.getCache("org.dspace.aztest.new", null);
        assertNotNull(c1);
        c1.put("AZ", "aaron.zeckoski");
        c1.put("BZ", "becky.zeckoski");
        assertEquals("aaron.zeckoski", c1.get("AZ"));
        assertEquals(null, c1.get("CZ"));
        assertEquals(2, c1.size());

        cachingService.resetCaches();

        assertEquals(null, c1.get("AZ"));
        assertEquals(0, c1.size());
        
        c1 = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.CachingServiceImpl#destroyCache(java.lang.String)}.
     */
    @Test
    public void testDestroyCache() {
        // destroy existing cache
        Cache cache = cachingService.getCache("org.dspace.caching.MemOnly", null);
        assertNotNull(cache);

        cachingService.destroyCache(cache.getName());

        Cache c2 = cachingService.getCache("org.dspace.caching.MemOnly", null);
        assertNotNull(c2);
        assertNotSame(cache, c2);

        // ok to destroy non-existent caches
        cachingService.destroyCache("XXXXXXXXXXXX");

        // destroy new cache
        Cache ca = cachingService.getCache("org.dspace.aztest", null);
        assertNotNull(ca);

        cachingService.destroyCache(ca.getName());

        Cache cb = cachingService.getCache("org.dspace.aztest", null);
        assertNotNull(cb);
        assertNotSame(ca, cb);
        
        //trash the references
        cache = c2 = ca = cb = null;
    }

}
