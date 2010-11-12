/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.text.NumberFormat;

/**
 * This class provides a number of tools that may be useful to the methods 
 * which generate the different types of report
 *
 * @author  Richard Jones
 */
public class ReportTools
{
    /**
     * method to take the given integer and produce a string to be used in
     * the display of the report.  Basically provides an interface for a
     * standard NumberFormat class, but without the hassle of instantiating
     * and localising it.
     *
     * @param   number  the integer to be formatted
     *
     * @return      a string containing the formatted number
     */
    public static String numberFormat(int number)
    {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        return nf.format((double) number);
    }
    
}
