/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests DCSeriesNumber class
 * @author pvillega
 */
public class DCSeriesNumberTest
{

    /**
     * Object to use in the tests
     */
    private DCSeriesNumber dc;


    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    public void init()
    {
        dc = new DCSeriesNumber();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    public void destroy()
    {
        dc = null;
    }

    /**
     * Test of DCSeriesNumber constructor, of class DCSeriesNumber.
     */
    @Test
    public void testDCSeriesNumber()
    {
        dc = new DCSeriesNumber();
        assertThat("testDCSeriesNumber 0", dc.getNumber(), equalTo(""));
        assertThat("testDCSeriesNumber 1", dc.getSeries(), equalTo(""));
    }

    /**
     * Test of DCSeriesNumber constructor, of class DCSeriesNumber.
     */
    @Test
    public void testDCSeriesNumberValue()
    {
        dc = new DCSeriesNumber(null);
        assertThat("testDCSeriesNumberValue 0", dc.getNumber(), equalTo(""));
        assertThat("testDCSeriesNumberValue 1", dc.getSeries(), equalTo(""));

        dc = new DCSeriesNumber("series");
        assertThat("testDCSeriesNumberValue 2", dc.getNumber(),
                equalTo(""));
        assertThat("testDCSeriesNumberValue 3", dc.getSeries(),
                equalTo("series"));

        dc = new DCSeriesNumber("series;number");
        assertThat("testDCSeriesNumberValue 4", dc.getNumber(),
                equalTo("number"));
        assertThat("testDCSeriesNumberValue 5", dc.getSeries(),
                equalTo("series"));

        dc = new DCSeriesNumber("series;number;number2");
        assertThat("testDCSeriesNumberValue 6", dc.getNumber(),
                equalTo("number;number2"));
        assertThat("testDCSeriesNumberValue 7", dc.getSeries(),
                equalTo("series"));
    }

    /**
     * Test of DCSeriesNumber constructor, of class DCSeriesNumber.
     */
    @Test
    public void testDCSeriesNumberValues()
    {
        dc = new DCSeriesNumber(null, null);
        assertThat("testDCSeriesNumberValues 0", dc.getNumber(), equalTo(""));
        assertThat("testDCSeriesNumberValues 1", dc.getSeries(), equalTo(""));

        dc = new DCSeriesNumber(null, "number");
        assertThat("testDCSeriesNumberValues 2", dc.getNumber(),
                equalTo("number"));
        assertThat("testDCSeriesNumberValues 3", dc.getSeries(),
                equalTo(""));

        dc = new DCSeriesNumber("series", null);
        assertThat("testDCSeriesNumberValues 4", dc.getNumber(),
                equalTo(""));
        assertThat("testDCSeriesNumberValues 5", dc.getSeries(),
                equalTo("series"));

        dc = new DCSeriesNumber("series", "number");
        assertThat("testDCSeriesNumberValues 6", dc.getNumber(),
                equalTo("number"));
        assertThat("testDCSeriesNumberValues 7", dc.getSeries(),
                equalTo("series"));
    }

    /**
     * Test of toString method, of class DCSeriesNumber.
     */
    @Test
    public void testToString()
    {
         dc = new DCSeriesNumber(null, null);
        assertThat("testToString 0", dc.toString(), nullValue());

        dc = new DCSeriesNumber(null, "number");
        assertThat("testToString 1", dc.toString(), nullValue());

        dc = new DCSeriesNumber("series", null);
        assertThat("testToString 2", dc.toString(), equalTo("series"));

        dc = new DCSeriesNumber("series", "number");
        assertThat("testToString 3", dc.toString(), equalTo("series;number"));
    }

    /**
     * Test of getSeries method, of class DCSeriesNumber.
     */
    @Test
    public void testGetSeries()
    {
        dc = new DCSeriesNumber(null, null);
        assertThat("testGetSeries 0", dc.getSeries(), equalTo(""));

        dc = new DCSeriesNumber("series", null);
        assertThat("testGetSeries 1", dc.getSeries(), equalTo("series"));
    }

    /**
     * Test of getNumber method, of class DCSeriesNumber.
     */
    @Test
    public void testGetNumber()
    {
        dc = new DCSeriesNumber(null, null);
        assertThat("testGetNumber 0", dc.getNumber(), equalTo(""));

        dc = new DCSeriesNumber(null, "number");
        assertThat("testGetNumber 1", dc.getNumber(), equalTo("number"));
    }
}
