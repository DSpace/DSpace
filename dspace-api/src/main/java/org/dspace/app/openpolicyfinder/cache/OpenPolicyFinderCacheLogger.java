/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openpolicyfinder.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * This is a EHCache listener responsible for logging Open Policy Finder cache events. It is
 * bound to the Open Policy Finder cache via the dspace/config/ehcache.xml file. We need a
 * dedicated Logger for each cache as the CacheEvent doesn't include details
 * about where the event occur
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 *
 */
public class OpenPolicyFinderCacheLogger implements CacheEventListener<Object, Object> {

    private static final Logger log = LogManager.getLogger(OpenPolicyFinderCacheLogger.class);

    @Override
    public void onEvent(CacheEvent<?, ?> cacheEvent) {
        log.debug("Open Policy Finder Cache Event Type: {} | Key: {} ",
            cacheEvent.getType(), cacheEvent.getKey());
    }

}