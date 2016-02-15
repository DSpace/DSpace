/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 * @author mhwood
 */
@RunWith(Parameterized.class)
public class MultiFormatDateParserTest
        extends AbstractUnitTest
{
    private String testMessage;
    private String toParseDate;
    private String expectedFormat;
    private boolean expectedResult;


    public MultiFormatDateParserTest(String testMessage, String toParseDate, String expectedFormat, boolean expectedResult)
    {
        this.testMessage = testMessage;
        this.toParseDate = toParseDate;
        this.expectedFormat = expectedFormat;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection dateFormatsToTest() {
       return Arrays.asList(new Object[][]{
               {"Should parse: yyyyMMdd", "19570127", "yyyyMMdd", true},
               {"Should parse: dd-MM-yyyy", "27-01-1957", "dd-MM-yyyy", true},
               {"Should parse: yyyy-MM-dd", "1957-01-27", "yyyy-MM-dd", true},
               {"Should parse: MM/dd/yyyy", "01/27/1957", "MM/dd/yyyy", true},
               {"Should parse: yyyy/MM/dd", "1957/01/27", "yyyy/MM/dd", true},
               {"Should parse: yyyyMMddHHmm", "195701272006", "yyyyMMddHHmm", true},
               {"Should parse: yyyyMMdd HHmm", "19570127 2006", "yyyyMMdd HHmm", true},
               {"Should parse: dd-MM-yyyy HH:mm", "27-01-1957 20:06", "dd-MM-yyyy HH:mm", true},
               {"Should parse: yyyy-MM-dd HH:mm", "1957-01-27 20:06", "yyyy-MM-dd HH:mm", true},
               {"Should parse: MM/dd/yyyy HH:mm", "01/27/1957 20:06", "MM/dd/yyyy HH:mm", true},
               {"Should parse: yyyy/MM/dd HH:mm", "1957/01/27 20:06", "yyyy/MM/dd HH:mm", true},
               {"Should parse: yyyyMMddHHmmss", "19570127200620", "yyyyMMddHHmmss", true},
               {"Should parse: yyyyMMdd HHmmss", "19570127 200620", "yyyyMMdd HHmmss", true},
               {"Should parse: dd-MM-yyyy HH:mm:ss", "27-01-1957 20:06:20", "dd-MM-yyyy HH:mm:ss", true},
               {"Should parse: MM/dd/yyyy HH:mm:ss", "01/27/1957 20:06:20", "MM/dd/yyyy HH:mm:ss", true},
               {"Should parse: yyyy/MM/dd HH:mm:ss", "1957/01/27 20:06:20", "yyyy/MM/dd HH:mm:ss", true},
               {"Should parse: yyyy MMM dd", "1957 Jan 27", "yyyy MMM dd", true},
               {"Should parse: yyyy-MM", "1957-01", "yyyy-MM", true},
               {"Should parse: yyyyMM", "195701", "yyyyMM", true},
               {"Should parse: yyyy", "1957", "yyyy", true},
               {"Should parse: yyyy-MM-dd'T'HH:mm:ss'Z'", "1957-01-27T12:34:56Z", "yyyy-MM-dd'T'HH:mm:ss'Z'", true},
               {"Should parse: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "1957-01-27T12:34:56.789Z", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", true},
               {"Shouldn't parse: yyyy/MM/ddHH:mm:ss", "1957/01/2720:06:20", "yyyy/MM/ddHH:mm:ss", false}
       });
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
    public void testParse() throws ParseException
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(expectedFormat);
        Date result = MultiFormatDateParser.parse(toParseDate);
        assertEquals(testMessage, expectedResult, simpleDateFormat.parse(toParseDate).equals(result));
    }
}
