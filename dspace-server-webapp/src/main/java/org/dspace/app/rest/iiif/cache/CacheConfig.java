/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring cache support. The configuration file is defined in
 * application properties.
 * <p>spring.cache.jcache.config=classpath:iiif/cache/ehcache.xml</p>
 * TODO: Before the cache is used in production there must be a way to
 * evict from the cache whenever a dspace item, bundle or bitstream is changed.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Spring boot cache configuration.
}
