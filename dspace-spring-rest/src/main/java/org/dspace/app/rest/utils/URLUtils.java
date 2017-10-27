package org.dspace.app.rest.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.dspace.core.Constants;

/**
 * Utility class to encode or decode URL
 */
public class URLUtils {

    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
}
