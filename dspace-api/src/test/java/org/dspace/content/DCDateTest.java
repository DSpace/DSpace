/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author pvillega
 */
public class DCDateTest
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
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    public void destroy()  {
        dc = null;
        c = null;
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDateDate()
    {
        dc = new DCDate((String)null);
        assertThat("testDCDateDate 1", dc.getYear(), equalTo(-1));
        assertThat("testDCDateDate 2", dc.getMonth(), equalTo(-1));
        assertThat("testDCDateDate 3", dc.getDay(), equalTo(-1));
        assertThat("testDCDateDate 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateDate 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateDate 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateDate 7", dc.getYearUTC(), equalTo(-1));
        assertThat("testDCDateDate 8", dc.getMonthUTC(), equalTo(-1));
        assertThat("testDCDateDate 9", dc.getDayUTC(), equalTo(-1));
        assertThat("testDCDateDate 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateDate 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateDate 12", dc.getSecondUTC(), equalTo(-1));

        // NB. Months begin at 0 in GregorianCalendar so 0 is January.
        c = new GregorianCalendar(2010,0,1);
        dc = new DCDate(c.getTime());

        assertThat("testDCDateDate 1 ", dc.getYear(), equalTo(2010));
        assertThat("testDCDateDate 2 ", dc.getMonth(), equalTo(1));
        assertThat("testDCDateDate 3 ", dc.getDay(), equalTo(1));
        assertThat("testDCDateDate 4 ", dc.getHour(), equalTo(0));
        assertThat("testDCDateDate 5 ", dc.getMinute(), equalTo(0));
        assertThat("testDCDateDate 6 ", dc.getSecond(), equalTo(0));

        assertThat("testDCDateDate 7 ", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateDate 8 ", dc.getMonthUTC(), equalTo(1));
        assertThat("testDCDateDate 9 ", dc.getDayUTC(), equalTo(1));
        assertThat("testDCDateDate 10 ", dc.getHourUTC(), equalTo(8));
        assertThat("testDCDateDate 11 ", dc.getMinuteUTC(), equalTo(0));
        assertThat("testDCDateDate 12 ", dc.getSecondUTC(), equalTo(0));

        c = new GregorianCalendar(2009,11,31,18,30);
        dc = new DCDate(c.getTime());

        assertThat("testDCDateDate 13 ", dc.getYear(), equalTo(2009));
        assertThat("testDCDateDate 14 ", dc.getMonth(), equalTo(12));
        assertThat("testDCDateDate 15 ", dc.getDay(), equalTo(31));
        assertThat("testDCDateDate 16 ", dc.getHour(), equalTo(18));
        assertThat("testDCDateDate 17 ", dc.getMinute(), equalTo(30));
        assertThat("testDCDateDate 18 ", dc.getSecondUTC(), equalTo(0));

        assertThat("testDCDateDate 19 ", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateDate 20 ", dc.getMonthUTC(), equalTo(1));
        assertThat("testDCDateDate 21 ", dc.getDayUTC(), equalTo(1));
        assertThat("testDCDateDate 22 ", dc.getHourUTC(), equalTo(2));
        assertThat("testDCDateDate 23 ", dc.getMinuteUTC(), equalTo(30));
        assertThat("testDCDateDate 24 ", dc.getSecondUTC(), equalTo(0));
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDateIntBits()
    {
        dc = new DCDate(2010,1,1,-1,-1,-1);

        assertThat("testDCDateIntBits 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateIntBits 2", dc.getMonth(), equalTo(1));
        assertThat("testDCDateIntBits 3", dc.getDay(), equalTo(1));
        assertThat("testDCDateIntBits 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateIntBits 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateIntBits 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateIntBits 8", dc.getMonthUTC(), equalTo(1));
        assertThat("testDCDateIntBits 9", dc.getDayUTC(), equalTo(1));
        assertThat("testDCDateIntBits 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateIntBits 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateIntBits 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate(2009,12,31,18,30,5);

        assertThat("testDCDateIntBits 13", dc.getYear(), equalTo(2009));
        assertThat("testDCDateIntBits 14", dc.getMonth(), equalTo(12));
        assertThat("testDCDateIntBits 15", dc.getDay(), equalTo(31));
        assertThat("testDCDateIntBits 16", dc.getHour(), equalTo(18));
        assertThat("testDCDateIntBits 17", dc.getMinute(), equalTo(30));
        assertThat("testDCDateIntBits 18", dc.getSecond(), equalTo(5));

        assertThat("testDCDateIntBits 19", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateIntBits 20", dc.getMonthUTC(), equalTo(1));
        assertThat("testDCDateIntBits 21", dc.getDayUTC(), equalTo(1));
        assertThat("testDCDateIntBits 22", dc.getHourUTC(), equalTo(2));
        assertThat("testDCDateIntBits 23", dc.getMinuteUTC(), equalTo(30));
        assertThat("testDCDateIntBits 24", dc.getSecondUTC(), equalTo(5));
        
    }

    /**
     * Test of DCDate constructor, of class DCDate.
     */
    @Test
    public void testDCDateString()
    {
        dc = new DCDate((String)null);
        assertThat("testDCDateString 1", dc.getYear(), equalTo(-1));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(-1));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(-1));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(-1));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(-1));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(-1));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate("");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(-1));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(-1));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(-1));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(-1));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(-1));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(-1));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate("2010");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(-1));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(-1));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(-1));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(-1));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate("2010-04");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(04));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(-1));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(04));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(-1));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate("2010-04-14");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(04));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(14));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(-1));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(-1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(-1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(04));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(14));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(-1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(-1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(-1));

        dc = new DCDate("2010-04-14T01");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(04));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(13));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(17));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(0));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(0));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(04));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(14));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(1));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(0));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(0));

        dc = new DCDate("2010-04-14T00:01");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(04));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(13));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(16));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(1));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(0));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(04));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(14));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(0));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(1));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(0));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDCDateString 1", dc.getYear(), equalTo(2010));
        assertThat("testDCDateString 2", dc.getMonth(), equalTo(04));
        assertThat("testDCDateString 3", dc.getDay(), equalTo(13));
        assertThat("testDCDateString 4", dc.getHour(), equalTo(16));
        assertThat("testDCDateString 5", dc.getMinute(), equalTo(0));
        assertThat("testDCDateIntBits 6", dc.getSecond(), equalTo(1));

        assertThat("testDCDateString 7", dc.getYearUTC(), equalTo(2010));
        assertThat("testDCDateString 8", dc.getMonthUTC(), equalTo(04));
        assertThat("testDCDateString 9", dc.getDayUTC(), equalTo(14));
        assertThat("testDCDateString 10", dc.getHourUTC(), equalTo(0));
        assertThat("testDCDateString 11", dc.getMinuteUTC(), equalTo(0));
        assertThat("testDCDateString 12", dc.getSecondUTC(), equalTo(1));
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
        assertThat("testToString 5", dc.toString(), equalTo("2010-04-14T01:00:00Z"));

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
     * Test of displayDate method, of class DCDate.
     */
    @Test
    public void testDisplayDate()
    {
        dc = new DCDate("2010");
        assertThat("testDisplayDate 1 ", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("2010"));

        dc = new DCDate("2010-04");
        assertThat("testDisplayDate 2 ", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("Apr-2010"));

        dc = new DCDate("2010-04-14");
        assertThat("testDisplayDate 3 ", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("14-Apr-2010"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDisplayDate 4 ", dc.displayDate(true, true,
                new Locale("en_GB")),
                equalTo("13-Apr-2010 16:00:01"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDisplayDate 5 ", dc.displayDate(false, true,
                new Locale("en_GB")),
                equalTo("13-Apr-2010"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDisplayDate 6 ", dc.displayDate(true, false,
                 new Locale("es")),
                 equalTo("14-abr-2010 00:00:01"));

        dc = new DCDate("2010-04-14T00:00:01Z");
        assertThat("testDisplayDate 7 ", dc.displayDate(false, false,
                new Locale("en_GB")),
                equalTo("14-Apr-2010"));
    }

    /**
     * Test of getCurrent method, of class DCDate.
     */
    @Test
    public void testGetCurrent()
    {
     Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(System.currentTimeMillis());
         calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
         assertTrue("testGetCurrent 0", DateUtils.isSameDay(DCDate.getCurrent().toDate(), calendar.getTime())); 
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
