/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.ezid;

import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Required;

/**
 * Create configured EZID requests.
 *
 * <p>Common EZID constant properties are:</p>
 *
 * <dl>
 *  <dt>EZID_SCHEME</dt>
 *  <dd>URL scheme (e.g. "https")</dd>
 *  <dt>EZID_HOST</dt>
 *  <dd>Name of the EZID API host</dd>
 *  <dt>EZID_PATH</dt>
 *  <dd>Path to the API endpoints</dd>
 * </dl>
 *
 * @author mwood
 */
public class EZIDRequestFactory
{
    private static String EZID_SCHEME;
    private static String EZID_HOST;
    private static String EZID_PATH;

    /**
     * Configure an EZID request.
     * 
     * @param requestPath specific request (DOI, shoulder).
     * @param username
     * @param password
     * @throws URISyntaxException 
     */
    public EZIDRequest getInstance(String requestPath, String username, String password)
            throws URISyntaxException
    {
        URIBuilder uri = new URIBuilder();

        uri.setScheme(EZID_SCHEME);

        uri.setHost(EZID_HOST);

        String head, tail;
        if (EZID_PATH.endsWith("/"))
            head = EZID_PATH.substring(0, EZID_PATH.length() - 1);
        else
            head = EZID_PATH;
        if (requestPath.startsWith("/"))
            tail = requestPath.substring( 0, requestPath.length() - 1);
        else
            tail = requestPath;

        StringBuilder path = new StringBuilder();
        path.append(head);
        path.append('/');
        path.append(tail);
        uri.setPath(path.toString());

        return new EZIDRequest(uri.build(), username, password);
    }

    /**
     * @param aEZID_SCHEME the EZID URL scheme to set
     */
    @Required
    public static void setEZID_SCHEME(String aEZID_SCHEME)
    {
        EZID_SCHEME = aEZID_SCHEME;
    }

    /**
     * @param aEZID_HOST the EZID host to set
     */
    @Required
    public static void setEZID_HOST(String aEZID_HOST)
    {
        EZID_HOST = aEZID_HOST;
    }

    /**
     * @param aEZID_PATH the EZID path to set
     */
    @Required
    public static void setEZID_PATH(String aEZID_PATH)
    {
        EZID_PATH = aEZID_PATH;
    }
}
