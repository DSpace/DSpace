/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.crosswalk.IConverter;

public class RemoveLastDotConverter implements IConverter
{
    public String makeConversion(String value)
    {
        if (StringUtils.isNotBlank(value) && value.endsWith("."))
        {
            return value.substring(0, value.length() - 1);
        }
        else
        {
            return value;
        }
    }
}
