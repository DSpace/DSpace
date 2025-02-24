/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.time.LocalDateTime;

/**
 * Various manipulations of dates and times.
 *
 * @author mwood
 */
public class TimeHelpers {
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
    public static LocalDateTime toMidnightUTC(LocalDateTime from) {
        return from.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}
