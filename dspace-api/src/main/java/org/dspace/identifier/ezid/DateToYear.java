/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.ezid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Convert a date-time string to the year thereof.
 *
 * @author mwood
 */
public class DateToYear
        implements Transform
{
    private static final SimpleDateFormat parser
            = new SimpleDateFormat("yyyy'-'MM'-'dd");

    @Override
    public synchronized String transform(String from)
            throws ParseException
    {
        Date when = parser.parse(from);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(when);
        return String.valueOf(calendar.get(Calendar.YEAR));
    }
}
