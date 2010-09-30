/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.services.caching;

import java.util.HashMap;
import java.util.Map;

import org.dspace.services.caching.model.MapCache;

/**
 * A simple ThreadLocal for holding maps of caches.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ThreadLocalMap extends ThreadLocal<Map<String, MapCache>> {

    @Override
    protected Map<String, MapCache> initialValue() {
        return new HashMap<String, MapCache>();
    }

}
