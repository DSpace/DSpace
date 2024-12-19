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
 * This is a EHCache listener responsible for logging LOBID "get by ID" cache events.
 * @see dspace/config/ehcache.xml
 *
 * @author Kim Shepherd
 */
public class LobidGetCacheLogger implements CacheEventListener<Object, Object> {

    private static final Logger log = LogManager.getLogger(LobidGetCacheLogger.class);

    @Override
    public void onEvent(CacheEvent<?, ?> cacheEvent) {
        log.debug("LOBID API Cache Event Type: {} | Key: {} ",
                cacheEvent.getType(), cacheEvent.getKey());
    }

}
