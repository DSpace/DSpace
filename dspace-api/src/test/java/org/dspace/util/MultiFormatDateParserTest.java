/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mhwood
 */
public class MultiFormatDateParserTest
        extends AbstractUnitTest
{
    public MultiFormatDateParserTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of parse method, of class MultiFormatDateParser.
     */
    @Test
    public void testParse()
    {
        System.out.println("parse");
        Calendar calendar = GregorianCalendar.getInstance(
                TimeZone.getTimeZone("UTC"),
                Locale.ENGLISH);
        String dateString;

        dateString = "1957-01-27";
        calendar.set(1957, Calendar.JANUARY, 27, 00, 00, 00);
        calendar.clear(Calendar.MILLISECOND);
        Date expResult = calendar.getTime();
        Date result = MultiFormatDateParser.parse(dateString);
        assertEquals(expResult, result);
    }
}
