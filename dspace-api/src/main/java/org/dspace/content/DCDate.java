/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

import org.apache.log4j.Logger;

// FIXME: Not very robust - assumes dates will always be valid

/**
 * Dublin Core date utility class
 * <P>
 * Dates in the DSpace database are held in the ISO 8601 format. They are always
 * stored in UTC, converting to and from the current time zone. In practice only dates
 * with a time component need to be converted.
 * <P>
 * <code>YYYY-MM-DDThh:mm:ss</code>
 * <P>
 * There are four levels of granularity, depending on how much date information
 * is available: year, month, day, time.
 * <P>
 * Examples: <code>1994-05-03T15:30:24</code>,<code>1995-10-04</code>,
 * <code>2001-10</code>,<code>1975</code>
 *
 * @author Robert Tansley
 * @author Larry Stone
 * @version $Revision$
 */
public class DCDate
{
    /** Logger */
    private static Logger log = Logger.getLogger(DCDate.class);

    // UTC timezone
    private static final TimeZone utcZone = TimeZone.getTimeZone("UTC");

    // components of time in UTC
    private GregorianCalendar calendar = null;

    // components of time in local zone
    private GregorianCalendar localCalendar = null;

    private enum DateGran { YEAR, MONTH, DAY, TIME }
    DateGran granularity = null;

    // Full ISO 8601 is e.g. "2009-07-16T13:59:21Z"
    private final SimpleDateFormat fullIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // without Z
    private final SimpleDateFormat fullIso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    // without seconds
    private final SimpleDateFormat fullIso3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    // without minutes
    private final SimpleDateFormat fullIso4 = new SimpleDateFormat("yyyy-MM-dd'T'HH");

    // Date-only ISO 8601 is e.g. "2009-07-16"
    private final SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");

    // Year-Month-only ISO 8601 is e.g. "2009-07"
    private final SimpleDateFormat yearMonthIso = new SimpleDateFormat("yyyy-MM");

    // just year, "2009"
    private final SimpleDateFormat yearIso = new SimpleDateFormat("yyyy");
    
    private static Map<Locale, DateFormatSymbols> dfsLocaleMap = new HashMap<Locale, DateFormatSymbols>();

    /**
     * Construct a date object from a Java <code>Date</code> object.
     *
     * @param date
     *            the Java <code>Date</code> object.
     */
    public DCDate(Date date)
    {
        setUTCForFormatting();

        if (date == null)
        {
            return;
        }

        // By definition a Date has a time component so always set the granularity to TIME.
        granularity = DateGran.TIME;

        // Set the local calendar.
        localCalendar = new GregorianCalendar();
        localCalendar.setTime(date);
        
        // Now set the UTC equivalent.
        calendar = new GregorianCalendar(utcZone);
        calendar.setTime(date);
     }

    /**
     * Construct a date object from a bunch of component parts. The date passed in is assumed to be in the current
     *  time zone. Unknown values should be given as -1.
     *
     * @param yyyy
     *            the year
     * @param mm
     *            the month
     * @param dd
     *            the day
     * @param hh
     *            the hours
     * @param mn
     *            the minutes
     * @param ss
     *            the seconds
     */
    public DCDate(int yyyy, int mm, int dd, int hh, int mn, int ss)
    {
        setUTCForFormatting();

        // default values
        int lyear = 0;
        int lhours = 0;
        int lminutes = 0;
        int lseconds = 0;
        int lmonth = 1;
        int lday = 1;

        if (yyyy > 0)
        {
            lyear = yyyy;
            granularity = DateGran.YEAR;
        }
        if (mm > 0)
        {
            lmonth = mm;
            granularity = DateGran.MONTH;
        }
        if (dd > 0)
        {
            lday = dd;
            granularity = DateGran.DAY;
        }
        if (hh >= 0)
        {
            lhours = hh;
            granularity = DateGran.TIME;
        }
        if (mn >= 0)
        {
            lminutes = mn;
            granularity = DateGran.TIME;
        }
        if (ss >= 0)
        {
            lseconds = ss;
            granularity = DateGran.TIME;
        }

        // Set the local calendar.
        localCalendar = new GregorianCalendar(lyear, lmonth - 1, lday,
                                                 lhours, lminutes, lseconds);

        if (granularity == DateGran.TIME)
        {
            // Now set the UTC equivalent.
            calendar = new GregorianCalendar(utcZone);
            calendar.setTime(localCalendar.getTime());
        }
        else
        {
            // No Time component so just set the UTC date to be the same as the local Year, Month, and Day.
            calendar = new GregorianCalendar(localCalendar.get(Calendar.YEAR), localCalendar.get(Calendar.MONTH), localCalendar.get(Calendar.DAY_OF_MONTH));
        }
    }

    /**
     * Construct a date from a Dublin Core value
     *
     * @param fromDC
     *            the date string, in ISO 8601 (no timezone, always use UTC)
     */
    public DCDate(String fromDC)
    {
        setUTCForFormatting();

        // An empty date is OK
        if ((fromDC == null) || fromDC.equals(""))
        {
            return;
        }

        // default granularity
        granularity = DateGran.TIME;
        Date date = tryParse(fullIso, fromDC);
        if (date == null)
        {
            date = tryParse(fullIso2, fromDC);
        }
        if (date == null)
        {
            date = tryParse(fullIso3, fromDC);
        }
        if (date == null)
        {
            date = tryParse(fullIso4, fromDC);
        }
        if (date == null)
        {
            // Seems there is no time component to the date.
            date = tryParse(dateIso, fromDC);
            if (date != null)
            {
                granularity = DateGran.DAY;
            }
        }
        if (date == null)
        {
            date = tryParse(yearMonthIso, fromDC);
            if (date != null)
            {
                granularity = DateGran.MONTH;
            }
        }
        if (date == null)
        {
            date = tryParse(yearIso, fromDC);
            if (date != null)
            {
                granularity = DateGran.YEAR;
            }
        }

        if (date == null)
        {
            log.warn("Mangled date: " + fromDC + "  ..failed all attempts to parse as date.");
        }
        else
        {
            // Set the UTC time.
            calendar = new GregorianCalendar(utcZone);
            calendar.setTime(date);

            // Now set the local equivalent.
            if (granularity == DateGran.TIME)
            {
                localCalendar = new GregorianCalendar();
                localCalendar.setTime(date);
            }
            else
            {
                // No Time component so just set the local date to be the same as the UTC  Year, Month, and Day.
                localCalendar = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
        }
    }

    /**
     * Set all the formatters to use UTC. SimpleDateFormat is not thread-safe which
     * is why they are not static variables initialised once.
     */
    private void setUTCForFormatting()
    {
        fullIso.setTimeZone(utcZone);
        fullIso2.setTimeZone(utcZone);
        fullIso3.setTimeZone(utcZone);
        fullIso4.setTimeZone(utcZone);
        dateIso.setTimeZone(utcZone);
        yearMonthIso.setTimeZone(utcZone);
        yearIso.setTimeZone(utcZone);
    }

    // Attempt to parse, swallowing errors; return null for failure.
    private synchronized Date tryParse(SimpleDateFormat sdf,  String source)
    {
        try
        {
            return sdf.parse(source);
        }
        catch (ParseException pe)
        {
            return null;
        }
    }
    
    /**
     * Get the year, adjusting for current time zone.
     *
     * @return the year
     */
    public int getYear()
    {
        return (!withinGranularity(DateGran.YEAR)) ? -1 : localCalendar.get(Calendar.YEAR);
    }

    /**
     * Get the month, adjusting for current time zone.
     *
     * @return the month
     */
    public int getMonth()
    {
        return (!withinGranularity(DateGran.MONTH)) ? -1 : localCalendar.get(Calendar.MONTH) + 1;
    }

    /**
     * Get the day, adjusting for current time zone.
     *
     * @return the day
     */
    public int getDay()
    {
        return (!withinGranularity(DateGran.DAY)) ? -1 : localCalendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the hour, adjusting for current time zone.
     *
     * @return the hour
     */
    public int getHour()
    {
        return  (!withinGranularity(DateGran.TIME)) ? -1 : localCalendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the minute, adjusting for current time zone.
     *
     * @return the minute
     */
    public int getMinute()
    {
        return (!withinGranularity(DateGran.TIME)) ? -1 : localCalendar.get(Calendar.MINUTE);
    }

    /**
     * Get the second, adjusting for current time zone.
     *
     * @return the second
     */
    public int getSecond()
    {
        return (!withinGranularity(DateGran.TIME)) ? -1 : localCalendar.get(Calendar.SECOND);
    }

    /**
     * Get the year in UTC.
     *
     * @return the year
     */
    public int getYearUTC()
    {
        return (!withinGranularity(DateGran.YEAR)) ? -1 : calendar.get(Calendar.YEAR);
    }

    /**
     * Get the month in UTC.
     *
     * @return the month
     */
    public int getMonthUTC()
    {
        return (!withinGranularity(DateGran.MONTH)) ? -1 : calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * Get the day in UTC.
     *
     * @return the day
     */
    public int getDayUTC()
    {
        return (!withinGranularity(DateGran.DAY)) ? -1 : calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the hour in UTC.
     *
     * @return the hour
     */
    public int getHourUTC()
    {
        return (!withinGranularity(DateGran.TIME)) ? -1 : calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the minute in UTC.
     *
     * @return the minute
     */
    public int getMinuteUTC()
    {
        return (!withinGranularity(DateGran.TIME)) ? -1 : calendar.get(Calendar.MINUTE);
    }

    /**
     * Get the second in UTC.
     *
     * @return the second
     */
    public int getSecondUTC()
    {
        return (!withinGranularity(DateGran.TIME)) ? -1 : calendar.get(Calendar.SECOND);
    }


    /**
     * Get the date as a string to put back in the Dublin Core. Use the UTC/GMT calendar version.
     *
     * @return The date as a string.
     */
    public String toString()
    {
        if (calendar == null)
        {
            return "null";
        }
        return toStringInternal();
    }

    private synchronized String toStringInternal()
    {
        if (granularity == DateGran.YEAR)
        {
            return String.format("%4d", getYearUTC());
        }
        else if (granularity == DateGran.MONTH)
        {
            return String.format("%4d-%02d", getYearUTC(), getMonthUTC());
        }
        else if (granularity == DateGran.DAY)
        {
            return String.format("%4d-%02d-%02d", getYearUTC(), getMonthUTC(), getDayUTC());
        }
        else
        {
            return fullIso.format(calendar.getTime());
        }
    }

    /**
     * Get the date as a Java Date object.
     *
     * @return a Date object
     */
    public Date toDate()
    {
        if (calendar == null)
        {
            return null;
        }
        else
        {
            return calendar.getTime();
        }
    }

    /**
     * Format a human-readable version of the DCDate, with optional time.
     * This needs to be in DCDate because it depends on the granularity of
     * the original time.
     *
     * FIXME: This should probably be replaced with a localized DateFormat.
     *
     * @param showTime
     *            if true, display the time with the date
     * @param isLocalTime
     *            if true, adjust for local time zone, otherwise UTC
     * @param locale
     *            locale of the user
     *
     * @return String with the date in a human-readable form.
     */
    public String displayDate(boolean showTime, boolean isLocalTime, Locale locale)
    {
        if (isLocalTime)
        {
            return displayLocalDate(showTime, locale);
        }
        else
        {
            return displayUTCDate(showTime, locale);
        }
    }

    public String displayLocalDate(boolean showTime, Locale locale)
    {
          // forcibly truncate month name to 3 chars -- XXX FIXME?
        String monthName = getMonthName(getMonth(), locale);
        if (monthName.length() > 2)
            monthName = monthName.substring(0, 3);

        // display date and time
        if (showTime && granularity == DateGran.TIME)
        {
            return String.format("%d-%s-%4d %02d:%02d:%02d", getDay(), monthName, getYear(), getHour(), getMinute(), getSecond());
        }
        else if (granularity == DateGran.YEAR)
        {
            return String.format("%4d", getYear());
        }
        else if (granularity == DateGran.MONTH)
        {
            return String.format("%s-%4d", monthName, getYear());
        }
        else
        {
            return String.format("%d-%s-%4d", getDay(), monthName, getYear());
        }
    }

    public String displayUTCDate(boolean showTime, Locale locale)
    {
        // forcibly truncate month name to 3 chars -- XXX FIXME?
        String monthName = getMonthName(getMonthUTC(), locale);
        if (monthName.length() > 2)
            monthName = monthName.substring(0, 3);

        // display date and time
        if (showTime && granularity == DateGran.TIME)
        {
            return String.format("%d-%s-%4d %02d:%02d:%02d", getDayUTC(), monthName, getYearUTC(), getHourUTC(), getMinuteUTC(), getSecondUTC());
        }
        else if (granularity == DateGran.YEAR)
        {
            return String.format("%4d", getYearUTC());
        }
        else if (granularity == DateGran.MONTH)
        {
            return String.format("%s-%4d", monthName, getYearUTC());
        }
        else
        {
            return String.format("%d-%s-%4d", getDayUTC(), monthName, getYearUTC());
        }
    }

     /**
      * Test if the requested level of granularity is within that of the date.
      *
      * @param dg
      *              The requested level of granularity.
      * @return
      *              true or false.
      *
      */
     private boolean withinGranularity(DateGran dg)
     {
         if (granularity == DateGran.TIME)
         {
             if ((dg == DateGran.TIME) || (dg == DateGran.DAY) || (dg == DateGran.MONTH) || (dg == DateGran.YEAR))
             {
                 return true;
             }
         }

         if (granularity == DateGran.DAY)
         {
             if ((dg == DateGran.DAY) || (dg == DateGran.MONTH) || (dg == DateGran.YEAR))
             {
                 return true;
             }
         }

         if (granularity == DateGran.MONTH)
         {
             if ((dg == DateGran.MONTH) || (dg == DateGran.YEAR))
             {
                 return true;
             }
         }

         if (granularity == DateGran.YEAR)
         {
             if (dg == DateGran.YEAR)
             {
                 return true;
             }
         }

         return false;
     }



    /**************  Some utility methods ******************/

    /**
     * Get a date representing the current instant in time.
     *
     * @return a DSpaceDate object representing the current instant.
     */
    public static DCDate getCurrent()
    {
        return (new DCDate(new Date()));
    }

    /**
     * Get a month's name for a month between 1 and 12. Any invalid month value
     * (e.g. 0 or -1) will return a value of "Unspecified".
     *
     * @param m
     *     the month number
     * @param locale
     *     which locale to render the month name in
     * @return the month name.
     */
    public static String getMonthName(int m, Locale locale)
    {
        if ((m > 0) && (m < 13))
        {
            DateFormatSymbols dfs = dfsLocaleMap.get(locale);
            if (dfs == null)
            {
                dfs = new DateFormatSymbols(locale);
                dfsLocaleMap.put(locale, dfs);
            }

           return dfs.getMonths()[m-1];
        }
        else
        {
            return "Unspecified";
        }

    }

}
