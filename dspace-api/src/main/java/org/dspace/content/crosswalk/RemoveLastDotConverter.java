package org.dspace.content.crosswalk;

import org.apache.commons.lang.StringUtils;

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
