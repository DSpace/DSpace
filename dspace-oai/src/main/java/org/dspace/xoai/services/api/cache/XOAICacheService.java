/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.cache;

import java.io.IOException;
import java.io.OutputStream;

import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMH;


public interface XOAICacheService {
    boolean isActive();

    boolean hasCache(String requestID);

    void handle(String requestID, OutputStream out) throws IOException;

    void store(String requestID, OAIPMH response) throws IOException;

    void delete(String requestID);

    void deleteAll() throws IOException;
}
