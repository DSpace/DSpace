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

    public static String format(Date date)
    {
        return format(date, true);
    }

    public static String format(Date date, boolean init)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.'000Z'");
        if (!init)
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'999Z'");
        // We indicate that the returned date is in Zulu time (UTC) so we have
        // to set the time zone of sdf correct.
        sdf.setTimeZone(TimeZone.getTimeZone("ZULU"));
        String ret = sdf.format(date);
        return ret;
    }

    public static Date parse(String date)
    {
        // in org.dspace.xoai.util.DateUtils.format(Date, boolean) we format all
        // dates with trailing milliseconds so we have to use them when parsing
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("ZULU"));
        Date ret;
        try
        {
            ret = format.parse(date);
            return ret;
        }
        catch (ParseException ex)
        {
            // 2008-01-01T00:00:00Z
            format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            format.setTimeZone(TimeZone.getTimeZone("ZULU"));
            try
            {
                return format.parse(date);
            }
            catch (ParseException e)
            {
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
                                log.error(e4.getMessage(), e);
                            }
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
