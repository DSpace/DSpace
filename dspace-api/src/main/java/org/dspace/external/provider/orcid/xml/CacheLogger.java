/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.orcid.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * A simple logger for cache events
 */
public class CacheLogger implements CacheEventListener<Object, Object> {
    private static final Logger log = LogManager.getLogger(CacheLogger.class);
    @Override
    public void onEvent(CacheEvent<?, ?> event) {
        log.debug("ORCID Cache Event Type: {} | Key: {} ",
                event.getType(), event.getKey());
    }
}
