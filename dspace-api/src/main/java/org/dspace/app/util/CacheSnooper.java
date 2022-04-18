/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.dspace.core.Context;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.CachingService;

/**
 * List all EhCache CacheManager and Cache instances.
 *
 * <p>This is a debugging tool, not used in the daily operation of DSpace.
 * Just run it from the installed instance using
 * {@code bin/dspace dsrun org.dspace.app.util.CacheSnooper}
 * to check that the cache configuration is what you expect it to be,
 * given your configuration.
 *
 * <p>This was created to prove a specific cache configuration patch,
 * but I leave it here in the hope that it may be useful to others.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CacheSnooper {
    private CacheSnooper() { }

    public static void main(String[] argv) {
        // Ensure that the DSpace kernel is started.
        DSpaceKernelImpl kernel = DSpaceKernelInit.getKernel(null);

        // Ensure that the services cache manager is started.
        CachingService serviceCaches = kernel.getServiceManager()
                .getServiceByName(null, CachingService.class);

        // Ensure that the database layer is started.
        Context ctx = new Context();

        // List those caches!
        for (CacheManager manager : CacheManager.ALL_CACHE_MANAGERS) {
            System.out.format("CacheManager:  %s%n", manager);
            for (String cacheName : manager.getCacheNames()) {
                Cache cache = manager.getCache(cacheName);
                System.out.format("       Cache:  '%s'; maxHeap:  %d; maxDisk:  %d%n",
                        cacheName,
                        cache.getCacheConfiguration().getMaxEntriesLocalHeap(),
                        cache.getCacheConfiguration().getMaxEntriesLocalDisk());
            }
        }
    }
}
