/**
 * $Id: ThreadLocalMap.java 3310 2008-11-20 10:21:15Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/services/caching/ThreadLocalMap.java $
 * ThreadLocalMap.java - DSpace2 - Oct 23, 2008 11:07:46 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
