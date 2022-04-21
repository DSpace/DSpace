/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus.service;

import java.util.Map;

/**
 * Interface for classes that allow to contact LiveImport clients.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface LiveImportClient {

    /**
     * Returns the response in String type converted from InputStream
     * 
     * @param timeout        The connect timeout in milliseconds
     * @param URL            URL
     * @param requestParams  This map contains the parameters to be included in the request.
     *                       Each parameter will be added to the url?(key=value)
     */
    public String executeHttpGetRequest(int timeout, String URL, Map<String, String> requestParams);

}