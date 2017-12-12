/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DateUtils
{

    private static Logger log = LogManager.getLogger(DateUtils.class);


    /**
     * Format a Date object as a valid UTC Date String, per OAI-PMH guidelines
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#DatestampsResponses
     *
     * @param date Date object
     * @return UTC date string
     */
    public static String format(Date date)
    {
        // NOTE: OAI-PMH REQUIRES that all dates be expressed in UTC format
        // as YYYY-MM-DDThh:mm:ssZ  For more details, see
        // http://www.openarchives.org/OAI/openarchivesprotocol.html#DatestampsResponses
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        // We indicate that the returned date is in Zulu time (UTC) so we have
        // to set the time zone of sdf correctly
        sdf.setTimeZone(TimeZone.getTimeZone("ZULU"));
        String ret = sdf.format(date);
        return ret;
    }

    /**
     * Parse a string into a Date object
     * @param date string to parse
     * @return Date
     */
    public static Date parse(String date)
    {
        // First try to parse as a full UTC date/time, e.g. 2008-01-01T00:00:00Z
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("ZULU"));
        Date ret;
        try
        {
            ret = format.parse(date);
            return ret;
        }
        catch (ParseException ex)
        {
            // If a parse exception, try other logical date/time formats
            // based on the local timezone
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault());
            try
            {
                return format.parse(date);
            }
            catch (ParseException e1)
            {
                format = new SimpleDateFormat("yyyy-MM-dd",
                        Locale.getDefault());
                try
                {
                    return format.parse(date);
                }
                catch (ParseException e2)
                {
                    format = new SimpleDateFormat("yyyy-MM",
                            Locale.getDefault());
                    try
                    {
                        return format.parse(date);
                    }
                    catch (ParseException e3)
                    {
                        format = new SimpleDateFormat("yyyy",
                                Locale.getDefault());
                        try
                        {
                            return format.parse(date);
                        }
                        catch (ParseException e4)
                        {
                            log.error(e4.getMessage(), e4);
                        }
                    }
                }
            }
        }
        return new Date();
    }

    public static Date parseFromSolrDate(String date)
    {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("ZULU"));
        Date ret;
        try
        {
            ret = format.parse(date);
            return ret;
        }
        catch (ParseException e)
        {
            log.error(e.getMessage(), e);
        }
        return new Date();
    }
}
