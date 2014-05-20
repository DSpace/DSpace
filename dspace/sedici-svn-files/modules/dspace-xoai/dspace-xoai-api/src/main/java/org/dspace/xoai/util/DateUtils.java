/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DateUtils
{

    private static Logger log = LogManager.getLogger(DateUtils.class);

    public static String formatToSolr(Date date)
    {
        // 2008-01-01T00:00:00Z
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String ret = format.format(date);
        return ret;
    }

    public static Date parseDate(String date)
    {
        // 2008-01-01T00:00:00Z
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        // format.setTimeZone(TimeZone.getTimeZone("ZULU"));
        Date ret;
        try
        {
            ret = format.parse(date);
            return ret;
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
                format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
        return new Date();
    }

    public static Date parseFromSolrDate(String date)
    {
        // 2008-01-01T00:00:00Z
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        // format.setTimeZone(TimeZone.getTimeZone("UTC"));
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
