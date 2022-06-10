/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.liveimportclient.service;

import java.util.Map;

/**
 * Interface for classes that allow to contact LiveImport clients.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface LiveImportClient {

    /**
     * Http GET request
     * 
     * @param timeout        The connect timeout in milliseconds
     * @param URL            URL
     * @param requestParams  This map contains the parameters to be included in the request.
     *                       Each parameter will be added to the url?(key=value)
     * @return               The response in String type converted from InputStream
     */
    public String executeHttpGetRequest(int timeout, String URL, Map<String, Map<String, String>> params);

    /**
     * Http POST request
     * 
     * @param URL      URL
     * @param params   This map contains the header params to be included in the request.
     * @param entry    the entity value
     * @return         the response in String type converted from InputStream
     */
    public String executeHttpPostRequest(String URL, Map<String, Map<String, String>> params, String entry);
}