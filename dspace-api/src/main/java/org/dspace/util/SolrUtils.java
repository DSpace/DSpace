/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Common constants and static methods for working with Solr.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class SolrUtils {
    /** Solr uses UTC always. */
    public static final ZoneId SOLR_TIME_ZONE = ZoneOffset.UTC;

    /** Restricted ISO 8601 format used by Solr. */
    public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** Do not instantiate. */
    private SolrUtils() { }

    /**
     * Create a formatter configured for Solr-style date strings and the UTC time zone.
     * @see #SOLR_DATE_FORMAT
     *
     * @return date formatter compatible with Solr.
     */
    public static DateTimeFormatter getDateFormatter() {
        // TODO: Can this be replaced with DateTimeFormatter.ISO_INSTANT?
        return DateTimeFormatter.ofPattern(SOLR_DATE_FORMAT).withZone(SOLR_TIME_ZONE);
    }

    /**
     * Maps target type into a string format.
     *
     * @param type a {@code String} that represents the date format style
     * @return {@code String} pattern for that type
     */
    public static String getDateformatFrom(String type) {
        String dateformatString = null;
        if ("DAY".equals(type)) {
            dateformatString = "dd-MM-yyyy";
        } else if ("MONTH".equals(type)) {
            dateformatString = "MMMM yyyy";
        } else if ("YEAR".equals(type)) {
            dateformatString = "yyyy";
        }
        return dateformatString;
    }
}
