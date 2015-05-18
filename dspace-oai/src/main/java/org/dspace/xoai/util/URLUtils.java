/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class URLUtils
{
    private static Logger log = LogManager.getLogger(URLUtils.class);

    public static String encode (String value) {
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            log.warn(e.getMessage(), e);
            return value;
        }
    }
}
