/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.ezid;

import java.net.URISyntaxException;
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

    /**
     * Configure an EZID request.
     *
     * @param authority our DOI authority number.
     * @param username EZID user name.
     * @param password {@code username}'s password.
     * @throws URISyntaxException
     */
    public EZIDRequest getInstance(String authority, String username, String password)
            throws URISyntaxException
    {
        return new EZIDRequest(EZID_SCHEME, EZID_HOST, authority, username, password);
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
}
