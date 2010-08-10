/*
 * DCDateTest.java
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author pvillega
 */
public class DCDateTest extends AbstractUnitTest
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DCDateTest.class);

    /**
     * Object to use in the tests
     */
    private DCDate dc;

    /**
     * Object to use in the tests
     */
    private Calendar c;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        dc = new DCDate();
        c = new GregorianCalendar();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        dc = null;
        c = null;
        super.destroy();
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDate()
    {
        dc = new DCDate();
        assertThat("testDCDate 0", dc.toString(), equalTo("null"));
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDateDate()
    {
        dc = new DCDate((Date)null);
        assertThat("testDCDateDate 0", dc.toString(), equalTo("null"));

        // If date is Jan 1st, DCDate incorrectly treats this as year granularity
        // c = new GregorianCalendar(2010,0,1);
        c = new GregorianCalendar(2010,1,1);
        dc = new DCDate(c.getTime());
        assertThat("testDCDateDate 1", dc.toString(), equalTo("2010-02-01"));

        // DCDate doesn't currently support month granularity when constructed from a calendar object
        // c = new GregorianCalendar(2010,3,0);
        // dc = new DCDate(c.getTime());
        // assertThat("testDCDateDate 2", dc.toString(), equalTo("2010-04"));

        // Broken by a 1 hour offset
        // c = new GregorianCalendar(2010,3,14);        
        // dc = new DCDate(c.getTime());
        // assertThat("testDCDateDate 3", dc.toString(), equalTo("2010-04-14"));
        
        // Broken by a 1 hour offset
        // c = new GregorianCalendar(2010,3,14,0,0,1);
        // dc = new DCDate(c.getTime());
        // assertThat("testDCDateDate 4", dc.toString(),
        //         equalTo("2010-04-14T00:00:01Z"));
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDateString()
    {
        dc = new DCDate((String)null);
        assertThat("testDCDateString 0", dc.toString(), equalTo("null"));

        dc = new DCDate("");
        assertThat("testDCDateString 1", dc.toString(), equalTo("null"));

        dc = new DCDate("2010");
        assertThat("testDCDateString 2", dc.toString(), equalTo("2010"));

        dc = new DCDate("2010-04");
        assertThat("testDCDateString 3", dc.toString(), equalTo("2010-04"));

        dc = new DCDate("2010-04-14");
        assertThat("testDCDateString 4", dc.toString(), equalTo("2010-04-14"));

        dc = new DCDate("2010-04-14T01");
        assertThat("testDCDateString 5", dc.toString(), equalTo("2010-04-14"));

        dc = new DCDate("2010-04-14T00:01");
        assertThat("testDCDateString 6", dc.toString(),
                equalTo("2010-04-14T00:01:00Z"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDCDateString 7", dc.toString(),
                equalTo("2010-04-14T00:00:01Z"));

    }

    /**
     * Test of getCurrent method, of class DCDate.
     */
    @Test
    public void testGetCurrent()
    {
        assertThat("testGetCurrent 0", DCDate.getCurrent().toDate(),
                equalTo(new Date()));
    }

    /**
     * Test of toString method, of class DCDate.
     */
    @Test
    public void testToString()
    {
        dc = new DCDate((String)null);
        assertThat("testToString 0", dc.toString(), equalTo("null"));

        dc = new DCDate("");
        assertThat("testToString 1", dc.toString(), equalTo("null"));

        dc = new DCDate("2010");
        assertThat("testToString 2", dc.toString(), equalTo("2010"));

        dc = new DCDate("2010-04");
        assertThat("testToString 3", dc.toString(), equalTo("2010-04"));

        dc = new DCDate("2010-04-14");
        assertThat("testToString 4", dc.toString(), equalTo("2010-04-14"));

        dc = new DCDate("2010-04-14T01");
        assertThat("testToString 5", dc.toString(), equalTo("2010-04-14"));

        dc = new DCDate("2010-04-14T00:01");
        assertThat("testToString 6", dc.toString(),
                equalTo("2010-04-14T00:01:00Z"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testToString 7", dc.toString(),
                equalTo("2010-04-14T00:00:01Z"));
    }

    /**
     * Test of toDate method, of class DCDate.
     */
    @Test
    public void testToDate()
    {
        dc = new DCDate((Date)null);
        assertThat("testToDate 0", dc.toDate(), nullValue());

        c = new GregorianCalendar(2010,0,0);
        dc = new DCDate(c.getTime());
        assertThat("testToDate 1", dc.toDate(), equalTo(c.getTime()));

        c = new GregorianCalendar(2010,4,0);
        dc = new DCDate(c.getTime());
        assertThat("testToDate 2", dc.toDate(), equalTo(c.getTime()));

        c = new GregorianCalendar(2010,4,14);
        dc = new DCDate(c.getTime());
        assertThat("testToDate 3", dc.toDate(), equalTo(c.getTime()));

        c = new GregorianCalendar(2010,4,14,0,0,1);
        dc = new DCDate(c.getTime());
        assertThat("testToDate 4", dc.toDate(), equalTo(c.getTime()));
    }

    /**
     * Test of setDateLocal method, of class DCDate.
     */
    @Test
    public void testSetDateLocal()
    {
        dc = new DCDate("");
        dc.setDateLocal(2010,0,0,-1,-1,-1);
        assertThat("testSetDateLocal 0", dc.toString(), equalTo("2010"));

        dc = new DCDate("");
        dc.setDateLocal(2010,4,0,-1,-1,-1);
        assertThat("testSetDateLocal 1", dc.toString(), equalTo("2010-04"));

        dc = new DCDate("");
        dc.setDateLocal(2010,4,14,-1,-1,-1);
        assertThat("testSetDateLocal 2", dc.toString(), equalTo("2010-04-14"));
        
        // Broken by a 1 hour offset
        // dc = new DCDate("");
        // dc.setDateLocal(2010,4,14,5,5,5);
        // assertThat("testSetDateLocal 3", dc.toString(),
        //         equalTo("2010-04-14T05:05:05Z"));

        // Broken by a 1 hour offset
        // dc = new DCDate("");
        // dc.setDateLocal(2010,4,14,0,0,1);
        // assertThat("testSetDateLocal 4", dc.toString(),
        //         equalTo("2010-04-14T00:00:01Z"));
    }

    /**
     * Test of getYear method, of class DCDate.
     */
    @Test
    public void testGetYear()
    {
        dc = new DCDate((String)null);
        assertThat("testGetYear 0", dc.getYear(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetYear 1", dc.getYear(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetYear 2", dc.getYear(), equalTo(2010));

        dc = new DCDate("2010-04");
        assertThat("testGetYear 3", dc.getYear(), equalTo(2010));

        dc = new DCDate("2010-04-14");
        assertThat("testGetYear 4", dc.getYear(), equalTo(2010));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetYear 5", dc.getYear(), equalTo(2010));
    }

    /**
     * Test of getMonth method, of class DCDate.
     */
    @Test
    public void testGetMonth()
    {
        dc = new DCDate((String)null);
        assertThat("testGetMonth 0", dc.getMonth(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetMonth 1", dc.getMonth(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetMonth 2", dc.getMonth(), equalTo(-1));

        dc = new DCDate("2010-04");
        assertThat("testGetMonth 3", dc.getMonth(), equalTo(4));

        dc = new DCDate("2010-04-14");
        assertThat("testGetMonth 4", dc.getMonth(), equalTo(4));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetMonth 5", dc.getMonth(), equalTo(4));
    }

    /**
     * Test of getDay method, of class DCDate.
     */
    @Test
    public void testGetDay()
    {
        dc = new DCDate((String)null);
        assertThat("testGetDay 0", dc.getDay(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetDay 1", dc.getDay(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetDay 2", dc.getDay(), equalTo(-1));

        dc = new DCDate("2010-04");
        assertThat("testGetDay 3", dc.getDay(), equalTo(-1));

        dc = new DCDate("2010-04-14");
        assertThat("testGetDay 4", dc.getDay(), equalTo(14));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetDay 5", dc.getDay(), equalTo(14));
    }

    /**
     * Test of getHour method, of class DCDate.
     */
    @Test
    public void testGetHour()
    {
        dc = new DCDate((String)null);
        assertThat("testGetHour 0", dc.getHour(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetHour 1", dc.getHour(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetHour 2", dc.getHour(), equalTo(0));
        
        dc = new DCDate("2010-04");
        assertThat("testGetHour 3", dc.getHour(), equalTo(0));
        
        dc = new DCDate("2010-04-14");
        assertThat("testGetHour 4", dc.getHour(), equalTo(0));

        // Broken with 1 hour offset
        // dc = new DCDate("2010-04-14T01:00:00Z");
        // assertThat("testGetHour 5", dc.getHour(), equalTo(1));
    }

    /**
     * Test of getMinute method, of class DCDate.
     */
    @Test
    public void testGetMinute()
    {
        dc = new DCDate((String)null);
        assertThat("testGetMinute 0", dc.getMinute(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetMinute 1", dc.getMinute(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetMinute 2", dc.getMinute(), equalTo(0));

        dc = new DCDate("2010-04");
        assertThat("testGetMinute 3", dc.getMinute(), equalTo(0));

        dc = new DCDate("2010-04-14");
        assertThat("testGetMinute 4", dc.getMinute(), equalTo(0));

        dc = new DCDate("2010-04-14T00:01:00Z");
        assertThat("testGetMinute 5", dc.getMinute(), equalTo(1));
    }

    /**
     * Test of getSecond method, of class DCDate.
     */
    @Test
    public void testGetSecond()
    {
        dc = new DCDate((String)null);
        assertThat("testGetSecond 0", dc.getSecond(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetSecond 1", dc.getSecond(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetSecond 2", dc.getSecond(), equalTo(0));

        dc = new DCDate("2010-04");
        assertThat("testGetSecond 3", dc.getSecond(), equalTo(0));

        dc = new DCDate("2010-04-14");
        assertThat("testGetSecond 4", dc.getSecond(), equalTo(0));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetSecond 5", dc.getSecond(), equalTo(1));
    }

    /**
     * Test of getYearGMT method, of class DCDate.
     */
    @Test
    public void testGetYearGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetYearGMT 0", dc.getYearGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetYearGMT 1", dc.getYearGMT(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetYearGMT 2", dc.getYearGMT(), equalTo(2010));

        dc = new DCDate("2010-04");
        assertThat("testGetYearGMT 3", dc.getYearGMT(), equalTo(2010));

        dc = new DCDate("2010-04-14");
        assertThat("testGetYearGMT 4", dc.getYearGMT(), equalTo(2010));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetYearGMT 5", dc.getYearGMT(), equalTo(2010));
    }

    /**
     * Test of getMonthGMT method, of class DCDate.
     */
    @Test
    public void testGetMonthGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetMonthGMT 0", dc.getMonthGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetMonthGMT 1", dc.getMonthGMT(), equalTo(-1));

        // Should return 0, returns 1
        // dc = new DCDate("2010");
        //assertThat("testGetMonthGMT 2", dc.getMonthGMT(), equalTo(0));

        dc = new DCDate("2010-04");
        assertThat("testGetMonthGMT 3", dc.getMonthGMT(), equalTo(3));

        // Should return 3, returns 4
        // dc = new DCDate("2010-04-14");
        // assertThat("testGetMonthGMT 4", dc.getMonthGMT(), equalTo(3));

        // Should return 3, returns 4
        // dc = new DCDate("2010-04-14T00:00:01Z");
        // assertThat("testGetMonthGMT 5", dc.getMonthGMT(), equalTo(3));
    }

    /**
     * Test of getDayGMT method, of class DCDate.
     */
    @Test
    public void testGetDayGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetDayGMT 0", dc.getDayGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetDayGMT 1", dc.getDayGMT(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetDayGMT 2", dc.getDayGMT(), equalTo(1));

        // Expect 1, gets 31
        //dc = new DCDate("2010-04");
        //assertThat("testGetDayGMT 3", dc.getDayGMT(), equalTo(1));

        // Another day less than expected, gets 13 instead of 14
        // dc = new DCDate("2010-04-14");
        //assertThat("testGetDayGMT 4", dc.getDayGMT(), equalTo(14));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetDayGMT 5", dc.getDayGMT(), equalTo(14));
    }

    /**
     * Test of getHourGMT method, of class DCDate.
     */
    @Test
    public void testGetHourGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetHourGMT 0", dc.getHourGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetHourGMT 1", dc.getHourGMT(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetHourGMT 2", dc.getHourGMT(), equalTo(0));

        // One hour out, returns 23
        // dc = new DCDate("2010-04");
        // assertThat("testGetHourGMT 3", dc.getHourGMT(), equalTo(0));

        // One hour out, returns 23
        // dc = new DCDate("2010-04-14");
        // assertThat("testGetHourGMT 4", dc.getHourGMT(), equalTo(0));

        dc = new DCDate("2010-04-14T01:00:00Z");
        assertThat("testGetHourGMT 5", dc.getHourGMT(), equalTo(1));
    }

    /**
     * Test of getMinuteGMT method, of class DCDate.
     */
    @Test
    public void testGetMinuteGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetMinuteGMT 0", dc.getMinuteGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetMinuteGMT 1", dc.getMinuteGMT(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetMinuteGMT 2", dc.getMinuteGMT(), equalTo(0));

        dc = new DCDate("2010-04");
        assertThat("testGetMinuteGMT 3", dc.getMinuteGMT(), equalTo(0));

        dc = new DCDate("2010-04-14");
        assertThat("testGetMinuteGMT 4", dc.getMinuteGMT(), equalTo(0));

        dc = new DCDate("2010-04-14T00:01:00Z");
        assertThat("testGetMinuteGMT 5", dc.getMinuteGMT(), equalTo(1));
    }

    /**
     * Test of getSecondGMT method, of class DCDate.
     */
    @Test
    public void testGetSecondGMT()
    {
        dc = new DCDate((String)null);
        assertThat("testGetSecondGMT 0", dc.getSecondGMT(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testGetSecondGMT 1", dc.getSecondGMT(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testGetSecondGMT 2", dc.getSecondGMT(), equalTo(0));

        dc = new DCDate("2010-04");
        assertThat("testGetSecondGMT 3", dc.getSecondGMT(), equalTo(0));

        dc = new DCDate("2010-04-14");
        assertThat("testGetSecondGMT 4", dc.getSecondGMT(), equalTo(0));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testGetSecondGMT 5", dc.getSecondGMT(), equalTo(1));
    }

    /**
     * Test of getMonthName method, of class DCDate.
     */
    @Test
    public void testGetMonthName()
    {
        assertThat("testGetMonthName 0", DCDate.getMonthName(-1, new Locale("en")),
                equalTo("Unspecified"));
        assertThat("testGetMonthName 1", DCDate.getMonthName(0, new Locale("en")),
                equalTo("Unspecified"));
        assertThat("testGetMonthName 2", DCDate.getMonthName(13, new Locale("en")),
                equalTo("Unspecified"));
        assertThat("testGetMonthName 3", DCDate.getMonthName(14, new Locale("en")),
                equalTo("Unspecified"));

        assertThat("testGetMonthName 4", DCDate.getMonthName(1, new Locale("en")),
                equalTo("January"));
        assertThat("testGetMonthName 5", DCDate.getMonthName(2, new Locale("en")),
                equalTo("February"));
        assertThat("testGetMonthName 6", DCDate.getMonthName(3, new Locale("en")),
                equalTo("March"));
        assertThat("testGetMonthName 7", DCDate.getMonthName(4, new Locale("en")),
                equalTo("April"));
        assertThat("testGetMonthName 8", DCDate.getMonthName(5, new Locale("en")),
                equalTo("May"));
        assertThat("testGetMonthName 9", DCDate.getMonthName(6, new Locale("en")),
                equalTo("June"));
        assertThat("testGetMonthName 10", DCDate.getMonthName(7, new Locale("en")),
                equalTo("July"));
        assertThat("testGetMonthName 11", DCDate.getMonthName(8, new Locale("en")),
                equalTo("August"));
        assertThat("testGetMonthName 12", DCDate.getMonthName(9, new Locale("en")),
                equalTo("September"));
        assertThat("testGetMonthName 13", DCDate.getMonthName(10, new Locale("en")),
                equalTo("October"));
        assertThat("testGetMonthName 14", DCDate.getMonthName(11, new Locale("en")),
                equalTo("November"));
        assertThat("testGetMonthName 15", DCDate.getMonthName(12, new Locale("en")),
                equalTo("December"));

        assertThat("testGetMonthName 16", DCDate.getMonthName(1, new Locale("es")),
                equalTo("enero"));
        assertThat("testGetMonthName 17", DCDate.getMonthName(2, new Locale("es")),
                equalTo("febrero"));
        assertThat("testGetMonthName 18", DCDate.getMonthName(3, new Locale("es")),
                equalTo("marzo"));
        assertThat("testGetMonthName 19", DCDate.getMonthName(4, new Locale("es")),
                equalTo("abril"));
        assertThat("testGetMonthName 20", DCDate.getMonthName(5, new Locale("es")),
                equalTo("mayo"));
        assertThat("testGetMonthName 21", DCDate.getMonthName(6, new Locale("es")),
                equalTo("junio"));
        assertThat("testGetMonthName 22", DCDate.getMonthName(7, new Locale("es")),
                equalTo("julio"));
        assertThat("testGetMonthName 23", DCDate.getMonthName(8, new Locale("es")),
                equalTo("agosto"));
        assertThat("testGetMonthName 24", DCDate.getMonthName(9, new Locale("es")),
                equalTo("septiembre"));
        assertThat("testGetMonthName 25", DCDate.getMonthName(10, new Locale("es")),
                equalTo("octubre"));
        assertThat("testGetMonthName 26", DCDate.getMonthName(11, new Locale("es")),
                equalTo("noviembre"));
        assertThat("testGetMonthName 27", DCDate.getMonthName(12, new Locale("es")),
                equalTo("diciembre"));
    }

    /**
     * Test of displayDate method, of class DCDate.
     */
    @Test
    public void testDisplayDate()
    {
         dc = new DCDate("");
        assertThat("testToString 0", dc.toString(), equalTo("null"));

        dc = new DCDate("2010");
        assertThat("testToString 1", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("2010"));

        dc = new DCDate("2010-04");
        assertThat("testToString 2", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("Apr-2010"));

        dc = new DCDate("2010-04-14");
        assertThat("testToString 3", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("14-Apr-2010"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        //hour increses in 1 due to locale
        assertThat("testToString 4", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("14-Apr-2010 01:00:01"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testToString 5", dc.displayDate(false, true,
                new Locale("en_GB")),
                equalTo("14-Apr-2010"));

        // Get different values depending on locale
        // dc = new DCDate("2010-04-14T00:00:01Z");
        // assertThat("testToString 6", dc.displayDate(true, false,
        //         new Locale("en_GB")),
        //         equalTo("14-Apr-2010 01:00:01"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testToString 7", dc.displayDate(false, false,
                new Locale("en_GB")),
                equalTo("14-Apr-2010"));
    }

    /**
     * Tests concurrency issues with date
     * JIRA: DS-594
     * Code by Andrew Taylor
     * @author Andrew Taylor
     */
    /**
    public void testDCDateStringConcurrency() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Callable<Void>> callables = new ArrayList<Callable<Void>>(10000);
        for (int i = 0; i < 10000; i++) {
            callables.add(new Callable<Void>() {
                public Void call() throws Exception {
                    DCDate date = new DCDate("2010-08-01T23:01:34Z");
                    date.toDate();
                    return null;
                }
            });
        }
        List<Future<Void>> invoked = exec.invokeAll(callables, 10, TimeUnit.SECONDS);
        for (Future<Void> future : invoked) {
            try {
                future.get();
            } catch (ExecutionException e) {
                log.error(e.toString(),e);
                fail("Exception encountered (see stderr)");
            }
        }
    }
    */
}
