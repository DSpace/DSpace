/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

public class StripLeadingNonAlphaNum implements TextFilter
{
    @Override
    public String filter(String str)
    {
        int i = 0;

        while (i < str.length() && !Character.isLetterOrDigit(str.charAt(i)))
        {
            i++;
        }

        if (i > 0)
        {
            return str.substring(i);
        }

        return str;
    }

    @Override
    public String filter(String str, String lang)
    {
        return filter(str);
    }
}
