package org.dspace.xoai.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class URLUtils
{
    private static Logger log = LogManager
            .getLogger(URLUtils.class);
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
