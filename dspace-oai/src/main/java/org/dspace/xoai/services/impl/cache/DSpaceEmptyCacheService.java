/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.cache;

import java.io.IOException;
import java.io.OutputStream;

import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMH;
import org.dspace.xoai.services.api.cache.XOAICacheService;

public class DSpaceEmptyCacheService implements XOAICacheService {
    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean hasCache(String requestID) {
        return false;
    }

    @Override
    public void handle(String requestID, OutputStream out) throws IOException {

    }

    @Override
    public void store(String requestID, OAIPMH response) throws IOException {

    }

    @Override
    public void delete(String requestID) {

    }

    @Override
    public void deleteAll() {

    }
}
