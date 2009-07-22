/**
 * $Id: ThreadLocalMap.java 3310 2008-11-20 10:21:15Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/services/caching/ThreadLocalMap.java $
 * ThreadLocalMap.java - DSpace2 - Oct 23, 2008 11:07:46 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services.caching;

import java.util.HashMap;
import java.util.Map;

import org.dspace.services.caching.model.MapCache;

/**
 * A simple threadlocal for holding maps of caches
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ThreadLocalMap extends ThreadLocal<Map<String, MapCache>> {

    @Override
    protected Map<String, MapCache> initialValue() {
        return new HashMap<String, MapCache>();
    }

}
