/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * This is a EHCache listener responsible for logging Geonames "get by ID" cache events.
 * @see dspace/config/ehcache.xml
 *
 * @author Kim Shepherd
 */
public class GeonamesGetCacheLogger implements CacheEventListener<Object, Object> {

    private static final Logger log = LogManager.getLogger(GeonamesGetCacheLogger.class);

    @Override
    public void onEvent(CacheEvent<?, ?> cacheEvent) {
        log.debug("Geonames API Cache Event Type: {} | Key: {} ",
                cacheEvent.getType(), cacheEvent.getKey());
    }

}
