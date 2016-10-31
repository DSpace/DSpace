/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

/**
 * Standard date ordering delegate implementation. The only "special" need is
 * to treat dates with less than 4-digit year.
 * 
 * @author Andrea Bollini
 */
public class OrderFormatDate implements OrderFormatDelegate
{
    @Override
    public String makeSortString(String value, String language)
    {
        int padding = 0;
        int endYearIdx = value.indexOf('-');

        if (endYearIdx >= 0 && endYearIdx < 4)
        {
            padding = 4 - endYearIdx;
        }
        else if (value.length() < 4)
        {
            padding = 4 - value.length();
        }

        if (padding > 0)
        {
            // padding the value from left with 0 so that 87 -> 0087, 687-11-24
            // -> 0687-11-24
            return String.format("%1$0" + padding + "d", 0)
                    + value;
        }
        else
        {
            return value;
        }
    }
}
