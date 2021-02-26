/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.core.Constants;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class URLUtils {
    private static Logger log = LogManager.getLogger(URLUtils.class);

    /**
     * Default constructor
     */
    private URLUtils() { }

    public static String encode(String value) {
        try {
            return Util.encodeBitstreamName(value, Constants.DEFAULT_ENCODING);

        } catch (UnsupportedEncodingException e) {
            log.warn(e.getMessage(), e);
            return value;
        }
    }
}
