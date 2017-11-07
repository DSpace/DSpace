/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.DCValue;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class DSpaceDataConvert
{
    public static List<String> getValues(DCValue[] values)
    {
        List<String> result = new ArrayList<String>();
        for (DCValue dc : values)
            result.add(dc.value);
        return result;
    }

    public static String getValue(DCValue[] values)
    {
        if (values.length > 0)
            return values[0].value;
        return null;
    }
}
