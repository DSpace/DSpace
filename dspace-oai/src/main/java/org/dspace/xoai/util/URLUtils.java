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
import org.dspace.app.util.Util;
import org.dspace.core.Constants;
import java.io.UnsupportedEncodingException;

/**
 * 
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class URLUtils
{
    private static Logger log = LogManager.getLogger(URLUtils.class);

    public static String encode (String value) {
        try
        {
        	return Util.encodeBitstreamName(value, Constants.DEFAULT_ENCODING);

        }
        catch (UnsupportedEncodingException e)
        {
            log.warn(e.getMessage(), e);
            return value;
        }
    }
}
