/*
 * DCDate.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;

// FIXME: No tests
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
 * is available.
 * <P>
 * Examples: <code>1994-05-03T15:30:24</code>,<code>1995-10-04</code>,
 * <code>2001-10</code>,<code>1975</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DCDate
{
    /** Logger */
    private static Logger cat = Logger.getLogger(DCDate.class);

    /** The year, or -1 if none */
    private int year;

    /** The month, or -1 if none */
    private int month;

    /** The day, or -1 if none */
    private int day;

    /** Hours, -1 if none */
    private int hours;

    /** Minutes, -1 if none */
    private int minutes;

    /** seconds, -1 if none */
    private int seconds;

    /**
     * Calendar object for timezone conversion. Only used if the date has a time
     * component.
     */
    private GregorianCalendar localGC;

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
        // Set all fields to unknown
        year = month = day = hours = minutes = seconds = -1;
        localGC = null;
    }

    /**
     * Construct a date from a Dublin Core value
     * 
     * @param fromDC
     *            the date string, in ISO 8601 (no timezone, always use UTC/GMT)
     */
    public DCDate(String fromDC)
    {
        // Set all fields to unknown
        year = month = day = hours = minutes = seconds = -1;
        localGC = null;

        // An empty date is OK
        if ((fromDC == null) || fromDC.equals(""))
        {
            return;
        }

        try
        {
            switch (fromDC.length())
            {
            case 20:

                // Full date and time
                hours = Integer.parseInt(fromDC.substring(11, 13));
                minutes = Integer.parseInt(fromDC.substring(14, 16));
                seconds = Integer.parseInt(fromDC.substring(17, 19));

            case 10:

                // Just full date
                day = Integer.parseInt(fromDC.substring(8, 10));

            case 7:

                // Just year and month
                month = Integer.parseInt(fromDC.substring(5, 7));

            case 4:

                // Just the year
                year = Integer.parseInt(fromDC.substring(0, 4));
            }
        }
        catch (NumberFormatException e)
        {
            // Mangled date
            cat.warn("Mangled date: " + fromDC + "  Exception: " + e);
            year = month = day = hours = minutes = seconds = -1;
        }
    }

    /**
     * Construct a date object from a Java <code>Date</code> object.
     * 
     * @param date
     *            the Java <code>Date</code> object.
     */
    public DCDate(Date date)
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);

        // Set all fields
        setDateLocal(calendar.get(Calendar.YEAR),

        // Uses 1 to 12 implementation below instead of Java's
                // 0 to 11 convention
                calendar.get(Calendar.MONTH) + 1, calendar
                        .get(Calendar.DAY_OF_MONTH), calendar
                        .get(Calendar.HOUR_OF_DAY), calendar
                        .get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
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
        StringBuffer sb = new StringBuffer();

        if (year > 0)
        {
            sb.append(year);
        }

        if (month > 0)
        {
            sb.append('-').append(fleshOut(month));
        }

        if (day > 0)
        {
            sb.append('-').append(fleshOut(day));
        }

        if (hours >= 0)
        {
            sb.append("T").append(fleshOut(hours)).append(':').append(
                    fleshOut(minutes)).append(':').append(fleshOut(seconds))
                    .append("Z");
        }

        return (sb.toString());
    }

    /**
     * Get the date as a Java Date object
     * 
     * @return a Date object
     */
    public Date toDate()
    {
        GregorianCalendar utcGC = new GregorianCalendar(TimeZone
                .getTimeZone("UTC"));

        utcGC.set(year, month - 1, day, hours, minutes, seconds);

        return utcGC.getTime();
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
        year = month = day = hours = minutes = seconds = -1;

        if (yyyy > 0)
        {
            year = yyyy;
        }
        else
        {
            return;
        }

        if (mm > 0)
        {
            month = mm;
        }
        else
        {
            return;
        }

        if (dd > 0)
        {
            day = dd;
        }
        else
        {
            return;
        }

        if (hh == -1)
        {
            return;
        }

        // We have a time, so we need to do a timezone adjustment
        localGC = new GregorianCalendar(year, month - 1, day, hh, mn, ss);

        // Adjust to UTC
        GregorianCalendar utcGC = new GregorianCalendar(TimeZone
                .getTimeZone("UTC"));

        utcGC.setTime(localGC.getTime());

        year = utcGC.get(Calendar.YEAR);

        // Notation
        month = utcGC.get(Calendar.MONTH) + 1;
        day = utcGC.get(Calendar.DAY_OF_MONTH);
        hours = utcGC.get(Calendar.HOUR_OF_DAY);
        minutes = utcGC.get(Calendar.MINUTE);
        seconds = utcGC.get(Calendar.SECOND);
    }

    /**
     * Get the date as an array of ints, adjusted for the current timezone
     * 
     * @return the date an an array: ( year, month, day, hour, minute, seconds) -
     *         unset fields are given a value of -1.
     */
    private int[] getDateLocal()
    {
        // Handle simple date cases first - no timezone adjustment
        if (hours == -1)
        {
            return new int[] { year, month, day, -1, -1, -1 };
        }

        // We have a full time, adjust to current timezone
        if (localGC == null)
        {
            GregorianCalendar utcGC = new GregorianCalendar(TimeZone
                    .getTimeZone("UTC"));

            utcGC.set(year, month - 1, day, hours, minutes, seconds);
            localGC = new GregorianCalendar();
            localGC.setTime(utcGC.getTime());
        }

        return new int[] { localGC.get(Calendar.YEAR),
                localGC.get(Calendar.MONTH) + 1,
                localGC.get(Calendar.DAY_OF_MONTH),
                localGC.get(Calendar.HOUR_OF_DAY),
                localGC.get(Calendar.MINUTE), localGC.get(Calendar.SECOND) };
    }

    /**
     * Get the year, adjusting for current time zone.
     * 
     * @return the year
     */
    public int getYear()
    {
        return (getDateLocal())[0];
    }

    /**
     * Get the month, adjusting for current time zone.
     * 
     * @return the month
     */
    public int getMonth()
    {
        return (getDateLocal())[1];
    }

    /**
     * Get the day, adjusting for current time zone.
     * 
     * @return the day
     */
    public int getDay()
    {
        return (getDateLocal())[2];
    }

    /**
     * Get the hour, adjusting for current time zone.
     * 
     * @return the hour
     */
    public int getHour()
    {
        return (getDateLocal())[3];
    }

    /**
     * Get the minute, adjusting for current time zone.
     * 
     * @return the minute
     */
    public int getMinute()
    {
        return (getDateLocal())[4];
    }

    /**
     * Get the second, adjusting for current time zone.
     * 
     * @return the second
     */
    public int getSecond()
    {
        return (getDateLocal())[5];
    }

    /**
     * Get the date as an array of ints in GMT
     * 
     * @return the date an an array: ( year, month, day, hour, minute, seconds) -
     *         unset fields are given a value of -1.
     */
    private int[] getDateGMT()
    {
        return new int[] { year, month, day, hours, minutes, seconds };
    }

    /**
     * Get the year in GMT.
     * 
     * @return the year
     */
    public int getYearGMT()
    {
        return (getDateGMT())[0];
    }

    /**
     * Get the month in GMT.
     * 
     * @return the month
     */
    public int getMonthGMT()
    {
        return (getDateGMT())[1];
    }

    /**
     * Get the day in GMT.
     * 
     * @return the day
     */
    public int getDayGMT()
    {
        return (getDateGMT())[2];
    }

    /**
     * Get the hour in GMT.
     * 
     * @return the hour
     */
    public int getHourGMT()
    {
        return (getDateGMT())[3];
    }

    /**
     * Get the minute in GMT.
     * 
     * @return the minute
     */
    public int getMinuteGMT()
    {
        return (getDateGMT())[4];
    }

    /**
     * Get the second in GMT.
     * 
     * @return the second
     */
    public int getSecondGMT()
    {
        return (getDateGMT())[5];
    }

    /**
     * Flesh out a number to two digits
     * 
     * @param n
     *            the number
     * @return the number as a two-digit string
     */
    private String fleshOut(int n)
    {
        if (n < 10)
        {
            return "0" + n;
        }
        else
        {
            return String.valueOf(n);
        }
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
}
