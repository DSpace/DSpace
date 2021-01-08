/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Constants;

/**
 * Utility class to encode or decode URL
 */
public class URLUtils {

    /**
     * Default constructor
     */
    private URLUtils() { }

    /**
     * Decode a given URL
     * @param url URL
     * @return decoded URL
     */
    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Encode a given URL
     * @param url URL
     * @return encoded URL
     */
    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Is one URL a prefix of another?  Ignores credentials, fragments and queries.
     * @param candidate
     * @param pattern
     * @return {@code true} if the URLs have equal protocol, host and port,
     *         and {@code pattern}'s path {@code String.startsWith} {@code candidate}'s path.
     * @throws IllegalArgumentException if either URL is malformed.
     */
    public static boolean urlIsPrefixOf(String candidate, String pattern)
            throws IllegalArgumentException {
        URL candidateURL;
        URL patternURL;
        try {
            candidateURL = new URL(candidate);
            patternURL = new URL(pattern);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("the supplied uri is not valid: " + pattern);
        }
        int candidatePort = candidateURL.getPort();
        if (candidatePort < 0) {
            candidatePort = candidateURL.getDefaultPort();
        }
        int patternPort = patternURL.getPort();
        if (patternPort < 0) {
            patternPort = patternURL.getDefaultPort();
        }

        // FIXME paths should be compared element-by-element, not string-wise.
        return StringUtils.equals(patternURL.getProtocol(), candidateURL.getProtocol())
                && StringUtils.equals(patternURL.getHost(), candidateURL.getHost())
                && patternPort == candidatePort
                && StringUtils.startsWith(patternURL.getPath(), candidateURL.getPath());
    }
}
