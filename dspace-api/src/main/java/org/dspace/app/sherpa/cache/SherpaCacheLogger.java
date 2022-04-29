/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * Log Sherpa cache events
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 *
 */
public class SherpaCacheLogger implements CacheEventListener<Object, Object> {

    private static final Logger log = LogManager.getLogger(SherpaCacheLogger.class);

    @Override
    public void onEvent(CacheEvent<?, ?> cacheEvent) {
        log.debug("Sherpa Cache Event Type: {} | Key: {} ",
            cacheEvent.getType(), cacheEvent.getKey());
    }

}