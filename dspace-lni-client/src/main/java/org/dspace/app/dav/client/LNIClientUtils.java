/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.dav.client;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Convenience and Utility functions for DSpace Lightweight Network Interface
 * clients. This class should be included in the JAR prepared for Java-based
 * clients of the LNI, along with the generated SOAP code.
 * 
 * @author Larry Stone
 * @version $Revision$
 */
public class LNIClientUtils
{
    
    /** Depth of infinity in SOAP propfind(). */
    public static final int INFINITY = -1;

    /**
     * Make a URL to access LNI WebDAV resource, given a LNI SOAP "endpoint" URL
     * and a relative WebDAV URI. Since the LNI SOAP interface does not include
     * any means to submit or disseminate DSpace Items, it relies on the WebDAV
     * LNI's GET and PUT methods. To make a WebDAV request, the client must
     * first construct a WebDAV URL; this function makes that step much more
     * convenient.
     * <p>
     * This is the inverse of makeLNIURI.
     * <p>
     * Since the actual Web servlet supporting the LNI SOAP interface can also
     * respond to WebDAV GET and PUT methods, there is a straightforward way to
     * construct the URL:
     * <p>
     * 1. Remove the last pathname element from the SOAP "endpoint" URL,
     * including the '/' (slash) separator character.
     * <p>
     * 2. Append the "relative URI" returned e.g. by the SOAP
     * <code>lookup()</code> function to this URL.
     * <p>
     * 3. Add the packager specification and other query arguments, e.g. by
     * appending "?packager=METS"
     * 
     * @param endpoint full URL of LNI Soap endpoint, as used with SOAP.
     * @param davURI relative URI of DAV resource, as returned by lookup(). assumed
     * to start with "/". May be null.
     * @param packager name of packager to use to retrieve/submit Item, or null.
     * 
     * @return new URL.
     * 
     * @throws MalformedURLException if endpoint is unacceptable URL, or the resulting construct
     * is not an acceptable URL.
     */
    public static URL makeDAVURL(String endpoint, String davURI, String packager)
            throws MalformedURLException
    {
        /* chop off last path element */
        int s = endpoint.lastIndexOf('/');
        if (s < 0)
        {
            throw new MalformedURLException(
                    "Illegal LNI SOAP endpoint, no path separators (/) found: "
                            + endpoint);
        }

        /* paste up URL.. */
        String result = endpoint.substring(0, s);
        if (davURI != null)
        {
            result += davURI;
            if (packager != null)
            {
                result += "?package=" + packager;
            }
        }
        return new URL(result);
    }

    /**
     * Make a URL to access LNI WebDAV resource, given a LNI SOAP "endpoint" URL
     * and a relative WebDAV URI. This version takes only two arguments, leaving
     * out the packager option.
     * <p>
     * This is the inverse of makeLNIURI.
     * 
     * @param endpoint full URL of LNI Soap endpoint, as used with SOAP.
     * @param davURI relative URI of DAV resource, as returned by lookup(). assumed
     * to start with "/". May be null.
     * 
     * @return new URL
     * 
     * @throws MalformedURLException if endpoint is unacceptable URL, or the resulting construct
     * is not an acceptable URL.
     */
    public static URL makeDAVURL(String endpoint, String davURI)
            throws MalformedURLException
    {
        return makeDAVURL(endpoint, davURI, null);
    }

    /**
     * Translates a WebDAV URL, such as would be returned by the PUT method,
     * into a resource URI relative to the DAV root which can be passed to the
     * SOAP methods.
     * <p>
     * This is the inverse of makeDAVURL.
     * 
     * @param endpoint full URL of LNI Soap endpoint, as used with SOAP.
     * @param davurl the davurl
     * 
     * @return the string
     * 
     * @throws MalformedURLException the malformed URL exception
     */
    public static String makeLNIURI(String endpoint, String davurl)
            throws MalformedURLException
    {
        URL emptyUrl = makeDAVURL(endpoint, null, null);
        URL url = new URL(davurl);
        return url.getPath().substring(emptyUrl.getPath().length());
    }
}
