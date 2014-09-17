/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.caching;

import static org.junit.Assert.*;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.dspace.services.caching.model.EhcacheCache;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Testing the functionality of the DSpace caches
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class EhcacheCacheTest extends DSpaceAbstractKernelTest {

    static String cacheName = "org.dspace.aaronz.test.Cache";
    static CacheManager cacheManager;
    static Cache cache = null;

    @BeforeClass
    public static void initClass() {
        CachingServiceImpl cachingService = getService(CachingServiceImpl.class);
        cacheManager = cachingService.getCacheManager();
        cache = cachingService.getCache(cacheName, new CacheConfig(CacheScope.INSTANCE));
    }

    @AfterClass
    public static void tearDownClass() {
        if(cacheManager!=null)
            cacheManager.shutdown();
        cacheManager = null;
        cache = null;
    }
    
    @Before
    public void init() {
        cache.clear();
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#getCache()}.
     */
    @Test
    public void testGetCache() {
        assertTrue(cache instanceof EhcacheCache);
        assertNotNull( ((EhcacheCache) cache).getCache() );
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#EhcacheCache(net.sf.ehcache.Ehcache, org.dspace.services.model.CacheConfig)}.
     */
    @Test
    public void testEhcacheCache() {
        cacheManager.addCache("org.dspace.ehcache");
        Ehcache ehcache = cacheManager.getEhcache("org.dspace.ehcache");
        assertNotNull(ehcache);

        EhcacheCache cache = new EhcacheCache(ehcache, new CacheConfig(CacheScope.INSTANCE));
        assertEquals("org.dspace.ehcache", cache.getName());
        
        //trash the references
        ehcache = null;
        cache = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#getConfig()}.
     */
    @Test
    public void testGetConfig() {
        CacheConfig cacheConfig = cache.getConfig();
        assertNotNull(cacheConfig);
        
        cacheConfig = null;
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#getName()}.
     */
    @Test
    public void testGetName() {
        String name = cache.getName();
        assertNotNull(name);
        assertEquals(cacheName, name);
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#clear()}.
     */
    @Test
    public void testClear() {
        cache.put("XX", "XXXX");
        assertTrue(cache.size() > 0);
        cache.clear();
        assertEquals(0, cache.size());
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#exists(java.lang.String)}.
     */
    @Test
    public void testExists() {
        assertFalse(cache.exists("XXX"));
        cache.put("XXX", 123);
        assertTrue(cache.exists("XXX"));
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#get(java.lang.String)}.
     */
    @Test
    public void testGet() {
        cache.put("XXX", 123);
        Integer i = (Integer) cache.get("XXX");
        assertNotNull(i);
        assertEquals(new Integer(123), i);

        Object o = cache.get("YYYYYYYY");
        assertNull(o);

        try {
           cache.get(null);
           fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
           assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#look(java.lang.String)}.
     */
    @Test
    public void testLook() {
        cache.put("XXX", "AZ");
        String thing = (String) cache.look("XXX");
        assertNotNull(thing);
        assertEquals("AZ", thing);

        // TODO better way to test this
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#put(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testPut() {
        assertEquals(0, cache.size());
        cache.put("XXX", 123);
        assertEquals(1, cache.size());      
        cache.put("YYY", null);
        assertEquals(2, cache.size());
        cache.put("XXX", "ABC");
        assertEquals(2, cache.size());
        cache.put("XXX", null);
        assertEquals(2, cache.size());
        Object o = cache.get("XXX");
        assertNull(o);

        try {
           cache.put(null, "XXX");
           fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
           assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#remove(java.lang.String)}.
     */
    @Test
    public void testRemove() {
        cache.put("XXX", 123);
        cache.put("YYY", null);
        assertEquals(2, cache.size());
        cache.remove("XXX");
        assertEquals(1, cache.size());
        cache.remove("XXX");
        assertEquals(1, cache.size());
        cache.remove("ZZZ");
        assertEquals(1, cache.size());

        try {
           cache.remove(null);
           fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
           assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.services.caching.model.EhcacheCache#size()}.
     */
    @Test
    public void testSize() {
        assertEquals(0, cache.size());
        cache.put("A", "AASSDDFF");
        assertEquals(1, cache.size());
        cache.put("B", "AASSDDFF");
        cache.put("C", "AASSDDFF");
        assertEquals(3, cache.size());
    }

}
