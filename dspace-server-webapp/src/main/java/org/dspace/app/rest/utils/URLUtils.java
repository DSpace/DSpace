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
     * @param pattern the potential prefix.
     * @param candidate does this URL match the pattern?
     * @return {@code true} if the URLs have equal protocol, host and port,
     *         and each element of {@code candidate}'s path {@link String#equals}
     *         the corresponding element in {@code pattern}'s path.
     * @throws IllegalArgumentException if either URL is malformed.
     */
    public static boolean urlIsPrefixOf(String pattern, String candidate)
            throws IllegalArgumentException {
        URL patternURL;
        URL candidateURL;

        try {
            patternURL = new URL(pattern);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The pattern URL is not valid:  " + pattern);
        }

        try {
            candidateURL = new URL(candidate);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The candidate URL is not valid:  " + candidate);
        }

        // Deal with port defaults.
        int patternPort = patternURL.getPort();
        if (patternPort < 0) {
            patternPort = patternURL.getDefaultPort();
        }
        int candidatePort = candidateURL.getPort();
        if (candidatePort < 0) {
            candidatePort = candidateURL.getDefaultPort();
        }

        boolean isPrefix;
        isPrefix = StringUtils.equals(candidateURL.getProtocol(), patternURL.getProtocol());
        isPrefix &= StringUtils.equals(candidateURL.getHost(), patternURL.getHost());
        isPrefix &= candidatePort == patternPort;

        String[] candidateElements = StringUtils.split(candidateURL.getPath(), '/');
        String[] patternElements = StringUtils.split(patternURL.getPath(), '/');

        // Candidate path cannot be shorter than pattern path.
        if (patternElements.length > candidateElements.length) {
            return false;
        }
        for (int elementN = 0; elementN < patternElements.length; elementN++) {
            isPrefix &= candidateElements[elementN].equals(patternElements[elementN]);
        }

        return isPrefix;
    }
}
