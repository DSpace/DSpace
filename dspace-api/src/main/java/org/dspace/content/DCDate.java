/*
 * DCDate.java
 *
 * Version: $Revision: 4892 $
 *
 * Date: $Date: 2010-05-05 15:21:33 -0400 (Wed, 05 May 2010) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import org.dspace.core.I18nUtil;

// FIXME: Not very robust - assumes dates will always be valid

/**
 * Dublin Core date utility class
 * <P>
 * Dates in the DSpace database are held in the ISO 8601 format. They are always
 * stored in UTC, converting to and from the current time zone.
 * <P>
 * <code>YYYY-MM-DDThh:mm:ss</code>
 * <P>
 * There are four levels of granularity, depending on how much date information
 * is available: year, month, day, time.
 * <P>
 * Examples: <code>1994-05-03T15:30:24</code>,<code>1995-10-04</code>,
 * <code>2001-10</code>,<code>1975</code>
 *
 * The main() method is a simple test program: run it with an optional
 * first argument that is a date string to decode, and it prints the
 * results of all the accessor methods.
 *
 * @author Robert Tansley
 * @author Larry Stone
 * @version $Revision: 4892 $
 */
public class DCDate
{
    /** Logger */
    private static Logger log = Logger.getLogger(DCDate.class);

    // UTC timezone
    private static final TimeZone utcZone = TimeZone.getTimeZone("UTC");

    // local timezone
    private static final TimeZone localZone = new GregorianCalendar().getTimeZone();

    // Full ISO 8601 is e.g. "2009-07-16T13:59:21Z"
    private static final SimpleDateFormat fullIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static { fullIso.setTimeZone(utcZone); }

    // without Z
    private static final SimpleDateFormat fullIso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static { fullIso2.setTimeZone(utcZone); }

    // without seconds
    private static final SimpleDateFormat fullIso3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    static { fullIso3.setTimeZone(utcZone); }

    // Date-only ISO 8601 is e.g. "2009-07-16"
    private static final SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
    static { dateIso.setTimeZone(utcZone); }

    // Year-Month-only ISO 8601 is e.g. "2009-07"
    private static final SimpleDateFormat yearMonthIso = new SimpleDateFormat("yyyy-MM");
    static { yearMonthIso.setTimeZone(utcZone); }

    // just year, "2009"
    private static final SimpleDateFormat yearIso = new SimpleDateFormat("yyyy");
    static { yearIso.setTimeZone(utcZone); }

    // components of time in UTC
    private GregorianCalendar calendar = null;

    // components of time in local zone, if needed
    private GregorianCalendar localCalendar = null;

    private enum DateGran { YEAR, MONTH, DAY, TIME };
    DateGran granularity = null;

   /**
    * DateFormatSymbols for locale monthsname
    */
    private static DateFormatSymbols dfs = null;
        
   /**
     * note the session locale
     */
    private static Locale langMonth = null;
    
        
    /**
     * Construct a clean date
     */
    public DCDate()
    {
        super();
    }

    /**
     * Construct a date object from a Java <code>Date</code> object.
     *
     * @param date
     *            the Java <code>Date</code> object.
     */
    public DCDate(Date date)
    {
        super();
        setTime(date);
        if (!(calendar.get(Calendar.HOUR_OF_DAY) == 0 &&
              calendar.get(Calendar.MINUTE) == 0 &&
              calendar.get(Calendar.SECOND) == 0))
            granularity = DateGran.TIME;

        // if date is 1-jan, assume it's because this was set for year
        else if (calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == 0)
            granularity = DateGran.YEAR;

        // otherwise day
        else
            granularity = DateGran.DAY;
    }

    /**
     * Construct a date from a Dublin Core value
     *
     * @param fromDC
     *            the date string, in ISO 8601 (no timezone, always use UTC/GMT)
     */
    public DCDate(String fromDC)
    {
        super();

        // An empty date is OK
        if ((fromDC == null) || fromDC.equals(""))
        {
            return;
        }
        // default granularity
        granularity = DateGran.TIME;
        Date date = tryParse(fullIso, fromDC);
        if (date == null)
            date = tryParse(fullIso2, fromDC);
        if (date == null)
            date = tryParse(fullIso3, fromDC);
        if (date == null)
        {
            // NOTE: move GMT date to local midnight when granularity is coarse
            date = tryParse(dateIso, fromDC);
            if (date != null)
            {
                long ldate = date.getTime();
                date = new Date(ldate - localZone.getOffset(ldate));
                granularity = DateGran.DAY;
            }
        }
        if (date == null)
        {
            // NOTE: move GMT date to local midnight when granularity is coarse
            date = tryParse(yearMonthIso, fromDC);
            if (date != null)
            {
                long ldate = date.getTime();
                date = new Date(ldate - localZone.getOffset(ldate));
                granularity = DateGran.MONTH;
            }
        }
        if (date == null)
        {
            // NOTE: move GMT date to local midnight when granularity is coarse
            date = tryParse(yearIso, fromDC);
            if (date != null)
            {
                long ldate = date.getTime();
                date = new Date(ldate - localZone.getOffset(ldate));
                granularity = DateGran.YEAR;
            }
        }

        if (date == null)
            log.warn("Mangled date: " + fromDC + "  ..failed all attempts to parse as date.");
        else
            setTime(date);
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
     * Set the time components to reflect the absolute time in this Date.
     *
     * @param date
     *            the Java <code>Date</code> object.
     */
    private void setTime(Date date)
    {
        calendar = new GregorianCalendar(utcZone);
        calendar.setTime(date);
    }

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
     * Get the date as a string to put back in the Dublin Core
     *
     * @return The date as a string.
     */
    public String toString()
    {
        if (calendar == null)
            return "null";
        return toStringInternal();
    }

    // When granularity is "day" or more, show the _local-time_ day because
    // when the granularity was coarse the local time value was set.
    private synchronized String toStringInternal()
    {
        if (granularity == DateGran.YEAR)
            return String.format("%4d", getYear());
        else if (granularity == DateGran.MONTH)
            return String.format("%4d-%02d", getYear(), getMonth());
        else if (granularity == DateGran.DAY)
            return String.format("%4d-%02d-%02d", getYear(), getMonth(), getDay());
        else
            return fullIso.format(calendar.getTime());
    }

    /**
     * Get the date as a Java Date object
     *
     * @return a Date object
     */
    public Date toDate()
    {
        if (calendar == null)
            return null;
        else
            return calendar.getTime();
    }

    /**
     * Set the date. The date passed in is assumed to be in the current time
     * zone, and is adjusting to fit the current time zone. Unknown values
     * should be given as -1.
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
    public void setDateLocal(int yyyy, int mm, int dd, int hh, int mn, int ss)
    {
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

        // do the timezone adjustment: get Date and put it in UTC zone.
        GregorianCalendar localGC = new GregorianCalendar(lyear, lmonth - 1, lday,
                                        lhours, lminutes, lseconds);
        setTime(localGC.getTime());
    }

    // get cached calendar in local timezone
    private GregorianCalendar getLocalCalendar()
    {
        if (localCalendar == null)
        {
            if (calendar == null)
                return null;
            localCalendar = new GregorianCalendar();
            localCalendar.setTime(calendar.getTime());
        }
        return localCalendar;
    }

    /**
     * Get the year, adjusting for current time zone.
     *
     * @return the year
     */
    public int getYear()
    {
        return ((getLocalCalendar() == null) || (!withinGranularity(DateGran.YEAR))) ? -1 : localCalendar.get(Calendar.YEAR);
    }
                    
    /**
     * Get the month, adjusting for current time zone.
     *
     * @return the month
     */
    public int getMonth()
    {
        return ((getLocalCalendar() == null) || (!withinGranularity(DateGran.MONTH))) ? -1 : localCalendar.get(Calendar.MONTH) + 1;
    }

    /**
     * Get the day, adjusting for current time zone.
     *
     * @return the day
     */
    public int getDay()
    {
        return ((getLocalCalendar() == null) || (!withinGranularity(DateGran.DAY))) ? -1 : localCalendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the hour, adjusting for current time zone.
     *
     * @return the hour
     */
    public int getHour()
    {
        return getLocalCalendar() == null ? -1 : localCalendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the minute, adjusting for current time zone.
     *
     * @return the minute
     */
    public int getMinute()
    {
        return getLocalCalendar() == null ? -1 : localCalendar.get(Calendar.MINUTE);
    }

    /**
     * Get the second, adjusting for current time zone.
     *
     * @return the second
     */
    public int getSecond()
    {
        return getLocalCalendar() == null ? -1 : localCalendar.get(Calendar.SECOND);
    }

    /**
     * Get the year in GMT.
     *
     * @return the year
     */
    public int getYearGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.YEAR);
    }

    /**
     * Get the month in GMT.
     *
     * @return the month
     */
    public int getMonthGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * Get the day in GMT.
     *
     * @return the day
     */
    public int getDayGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the hour in GMT.
     *
     * @return the hour
     */
    public int getHourGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the minute in GMT.
     *
     * @return the minute
     */
    public int getMinuteGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.MINUTE);
    }

    /**
     * Get the second in GMT.
     *
     * @return the second
     */
    public int getSecondGMT()
    {
        return calendar == null ? -1 : calendar.get(Calendar.SECOND);
    }

    /**
     * Get a month's name for a month between 1 and 12. Any invalid month value
     * (e.g. 0 or -1) will return a value of "Unspecified".
     *
     * @param m
     *            the month number
     *
     * @return the month name.
     */
    public static String getMonthName(int m, Locale locale)
    {
        if ((m > 0) && (m < 13))
        {
            if (dfs == null || !langMonth.equals(locale))
                {
                        dfs = new DateFormatSymbols(locale);
                        langMonth = locale;
                }
           return dfs.getMonths()[m-1];
        }
        else
        {
            return "Unspecified";
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

    /**
     * Simple test program
     * Usage:  java org.dspace.content.DCdate [DCDate | -l yyyy [mm [dd ..]]] ]
     *  where "DCDate" is the kind of value that would be in metadata,
     *  e.g. "2006", "2006-02-03", etc.
     *  (-l form tests local time parsing)
     * Default is to use current time.
     */
    public static void main(String args[])
        throws Exception
    {
        DCDate d;

        // if there's an arg, parse it for the date, otherwise use now
        if (args.length > 0)
        {
            if (args[0].equals("-l"))
            {
                int val[] = { -1, -1, -1, -1, -1, -1 };
                for (int i = 1; i < 7 && i < args.length; ++i)
                    val[i-1] = Integer.parseInt(args[i]);
                d = new DCDate();
                d.setDateLocal(val[0], val[1], val[2], val[3], val[4], val[5]);
            }
            else
                d = new DCDate(args[0]);
        }
        else
            d = DCDate.getCurrent();

        // display results:
        System.out.println("toString() = \""+d.toString()+"\"");
        System.out.println("toDate().toString() = \""+d.toDate().toString()+"\"");
        System.out.println("\ndisplayDate(time=F,loc=F) = \""+d.displayDate(false, false, I18nUtil.DEFAULTLOCALE)+"\"");
        System.out.println("displayDate(time=T,loc=F) = \""+d.displayDate(true, false, I18nUtil.DEFAULTLOCALE)+"\"");
        System.out.println("displayDate(time=F,loc=T) = \""+d.displayDate(false, true,  I18nUtil.DEFAULTLOCALE)+"\"");
        System.out.println("displayDate(time=T,loc=T) = \""+d.displayDate(true, true,  I18nUtil.DEFAULTLOCALE)+"\"");

        System.out.println("By component:");
        System.out.println("granularity   = "+d.granularity);
        System.out.println("getYear(),    = "+d.getYear());
        System.out.println("getMonth(),   = "+d.getMonth());
        System.out.println("getDay(),     = "+d.getDay());
        System.out.println("getHour(),    = "+d.getHour());
        System.out.println("getMinute(),  = "+d.getMinute());
        System.out.println("getSecond()); = "+d.getSecond());
        System.out.println("By GMT component:");
        System.out.println("getYearGMT(),    = "+d.getYearGMT());
        System.out.println("getMonthGMT(),   = "+d.getMonthGMT());
        System.out.println("getDayGMT(),     = "+d.getDayGMT());
        System.out.println("getHourGMT(),    = "+d.getHourGMT());
        System.out.println("getMinuteGMT(),  = "+d.getMinuteGMT());
        System.out.println("getSecondGMT()); = "+d.getSecondGMT());

        // convert it the hard way:
        DCDate hw = new DCDate();
        hw.setDateLocal(d.getYear(),d.getMonth(),d.getDay(),
                        d.getHour(),d.getMinute(),d.getSecond());
        System.out.println("hardway.toString() = \""+hw.toString()+"\"");

        // month str
        System.out.println("Month Name   = \""+DCDate.getMonthName(d.getMonth(), Locale.getDefault())+"\"");
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
     *            if true, adjust for local time zone, otherwise GMT
     * @param locale
     *            locale of the user
     *
     * @return String with the date in a human-readable form.
     */
    public String displayDate(boolean showTime, boolean isLocalTime, Locale locale)
    {
        // if we are only showing day of a DCDate with time granularity,
        // create a temporary DCDate with date granularity so getDay() etc work.
        DCDate dd = this;
        if (!showTime && granularity == DateGran.TIME)
        {
            dd = new DCDate();
            dd.setDateLocal(getYearGMT(), getMonthGMT(), getDayGMT(), -1, -1, -1);
        }

        // forcibly truncate month name to 3 chars -- XXX FIXME?
        String monthName = DCDate.getMonthName(dd.getMonth(), locale);
        if (monthName.length()  > 2)
            monthName = monthName.substring(0, 3);

        // display date and time
        if (showTime && granularity == DateGran.TIME)
        {
            if (isLocalTime)
            {
                return String.format("%d-%s-%4d %02d:%02d:%02d",
                                     dd.getDay(), monthName, dd.getYear(),
                                     dd.getHour(), dd.getMinute(), dd.getSecond());
            }
            else
            {
                monthName = DCDate.getMonthName(dd.getMonthGMT(), locale);
                if (monthName.length()  > 2)
                    monthName = monthName.substring(0, 3);
                return String.format("%d-%s-%4d %02d:%02d:%02d",
                                     dd.getDayGMT(), monthName, dd.getYearGMT(),
                                     dd.getHourGMT(), dd.getMinuteGMT(), dd.getSecondGMT());
            }
        }
        else if (granularity == DateGran.DAY)
        {
            return String.format("%d-%s-%4d", dd.getDay(), monthName, dd.getYear());
        }
        else if (granularity == DateGran.MONTH)
        {
            return String.format("%s-%4d", monthName, dd.getYear());
        }
        else
        {
            return String.format("%4d", dd.getYear());
        }
    }
}
