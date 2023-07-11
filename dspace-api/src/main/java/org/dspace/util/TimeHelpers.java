/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Various manipulations of dates and times.
 *
 * @author mwood
 */
public class TimeHelpers {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Never instantiate this class.
     */
    private TimeHelpers() {}

    /**
     * Set a Date's time to midnight UTC.
     *
     * @param from some date-time.
     * @return midnight UTC of the supplied date-time.
     */
    public static Date toMidnightUTC(Date from) {
        GregorianCalendar calendar = new GregorianCalendar(UTC);
        calendar.setTime(from);
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
