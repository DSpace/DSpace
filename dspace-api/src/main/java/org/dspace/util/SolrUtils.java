/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Common constants and static methods for working with Solr.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class SolrUtils {
    /** Solr uses UTC always. */
    public static final TimeZone SOLR_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    /** Restricted ISO 8601 format used by Solr. */
    public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** Do not instantiate. */
    private SolrUtils() { }

    /**
     * Create a formatter configured for Solr-style date strings and the UTC time zone.
     * @see SOLR_DATE_FORMAT
     *
     * @return date formatter compatible with Solr.
     */
    public static DateFormat getDateFormatter() {
        return new SimpleDateFormat(SolrUtils.SOLR_DATE_FORMAT);
    }
}
