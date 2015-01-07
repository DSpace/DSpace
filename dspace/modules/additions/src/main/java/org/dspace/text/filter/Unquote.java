/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

public class Unquote implements TextFilter
{
    @Override
    public String filter(String str)
    {
        if ((str.startsWith("\"") && str.endsWith("\""))
                || (str.startsWith("'") && str.endsWith("'")))
        {
            if (str.length() == 2)
            {
                return "";
            }
            else if (str.length() > 2)
            {
                return str.substring(1, str.length() - 1);
            }
        }

        return str;
    }

    @Override
    public String filter(String str, String lang)
    {
        return filter(str);
    }
}
