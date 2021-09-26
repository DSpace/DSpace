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
 * application properties. Cache size limits are defined there.
 * <p>spring.cache.jcache.config=classpath:iiif/cache/ehcache.xml</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Spring boot cache configuration.
}
