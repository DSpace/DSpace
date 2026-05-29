/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DateUtils {

    private static Logger log = LogManager.getLogger(DateUtils.class);

    /**
     * Default constructor
     */
    private DateUtils() { }

    /**
     * Format a Date object as a valid UTC Date String, per OAI-PMH guidelines
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#DatestampsResponses
     *
     * @param date Instant object
     * @return UTC date string
     */
    public static String format(Instant date) {
        Instant truncated = date.truncatedTo(ChronoUnit.SECONDS);
        return DateTimeFormatter.ISO_INSTANT.format(truncated);
    }

    /**
     * Parse a string into a Date object
     *
     * @param date string to parse
     * @return Date
     */
    public static Instant parse(String date) {
        // First try to parse as a full UTC date/time, e.g. 2008-01-01T00:00:00Z
        DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;
        Instant ret;
        try {
            ret = format.parse(date, Instant::from);
            return ret;
        } catch (DateTimeParseException ex) {
            // If a parse exception, try other logical date/time formats
            // based on the local timezone
            format = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            try {
                return format.parse(date, Instant::from);
            } catch (DateTimeParseException e1) {
                format = DateTimeFormatter.ISO_LOCAL_DATE;
                try {
                    return format.parse(date, Instant::from);
                } catch (DateTimeParseException e2) {
                    format = DateTimeFormatter.ofPattern("yyyy-MM");
                    try {
                        return format.parse(date, Instant::from);
                    } catch (DateTimeParseException e3) {
                        format = DateTimeFormatter.ofPattern("yyyy");
                        try {
                            return format.parse(date, Instant::from);
                        } catch (DateTimeParseException e4) {
                            log.error(e4.getMessage(), e4);
                        }
                    }
                }
            }
        }
        return Instant.now();
    }

    public static Instant parseFromSolrDate(String date) {
        Instant ret;
        try {
            ret = Instant.parse(date);
            return ret;
        } catch (DateTimeParseException e) {
            log.error(e.getMessage(), e);
        }
        return Instant.now();
    }
}
