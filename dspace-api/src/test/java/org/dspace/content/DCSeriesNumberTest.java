/*
 * DCSeriesNumberTest.java
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

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests DCSeriesNumber class
 * @author pvillega
 */
public class DCSeriesNumberTest extends AbstractUnitTest
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
    @Override
    public void init()
    {
        super.init();
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
    @Override
    public void destroy()
    {
        dc = null;
        super.destroy();
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
