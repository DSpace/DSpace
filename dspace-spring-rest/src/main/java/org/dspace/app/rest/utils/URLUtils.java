/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
}
