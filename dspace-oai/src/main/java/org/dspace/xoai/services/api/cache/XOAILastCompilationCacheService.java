/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.cache;

import java.io.IOException;
import java.time.Instant;


public interface XOAILastCompilationCacheService {
    boolean hasCache();

    void put(Instant date) throws IOException;

    Instant get() throws IOException;
}
