/*
 * DCLanguageTest.java
 *
 * Version: $Revision: 4290 $
 *
 * Date: $Date: 2009-09-29 03:57:36 +0100 (Tue, 29 Sep 2009) $
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

import java.util.Locale;
import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;


/**
 * Tests DCLanguageTest class
 * @author pvillega
 */
public class DCLanguageTest extends AbstractUnitTest
{

    /**
     * Object to use in the tests
     */
    private DCLanguage dc;


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
        dc = new DCLanguage("");
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
     * Test of DCLanguage constructor, of class DCLanguage.
     */
    @Test
    public void testDCLanguage()
    {
        dc = new DCLanguage(null);
        assertThat("testDCLanguage 0", dc.toString(), equalTo(""));

        dc = new DCLanguage("");
        assertThat("testDCLanguage 1", dc.toString(), equalTo(""));

        dc = new DCLanguage("other");
        assertThat("testDCLanguage 2", dc.toString(), equalTo("other"));

        dc = new DCLanguage("12");
        assertThat("testDCLanguage 3", dc.toString(), equalTo("12"));

        dc = new DCLanguage("12345");
        assertThat("testDCLanguage 4", dc.toString(), equalTo("45_12"));

        dc = new DCLanguage("123456");
        assertThat("testDCLanguage 5", dc.toString(), equalTo(""));

        dc = new DCLanguage("1234567890");
        assertThat("testDCLanguage 6", dc.toString(), equalTo(""));
    }

    /**
     * Test of toString method, of class DCLanguage.
     */
    @Test
    public void testToString()
    {
        dc = new DCLanguage(null);
        assertThat("testToString 0", dc.toString(), equalTo(""));

        dc = new DCLanguage("");
        assertThat("testToString 1", dc.toString(), equalTo(""));

        dc = new DCLanguage("other");
        assertThat("testToString 2", dc.toString(), equalTo("other"));

        dc = new DCLanguage("12");
        assertThat("testToString 3", dc.toString(), equalTo("12"));

        dc = new DCLanguage("12345");
        assertThat("testToString 4", dc.toString(), equalTo("45_12"));

        dc = new DCLanguage("123456");
        assertThat("testToString 5", dc.toString(), equalTo(""));

        dc = new DCLanguage("1234567890");
        assertThat("testToString 6", dc.toString(), equalTo(""));
    }

    /**
     * Test of setLanguage method, of class DCLanguage.
     */
    @Test
    public void testSetLanguage()
    {
        dc = new DCLanguage(null);
        assertThat("testSetLanguage 0", dc.toString(), equalTo(""));

        dc = new DCLanguage("");
        assertThat("testSetLanguage 1", dc.toString(), equalTo(""));

        dc = new DCLanguage("other");
        assertThat("testSetLanguage 2", dc.toString(), equalTo("other"));

        dc = new DCLanguage("12");
        assertThat("testSetLanguage 3", dc.toString(), equalTo("12"));

        dc = new DCLanguage("12345");
        assertThat("testSetLanguage 4", dc.toString(), equalTo("45_12"));

        dc = new DCLanguage("123456");
        assertThat("testSetLanguage 5", dc.toString(), equalTo(""));

        dc = new DCLanguage("1234567890");
        assertThat("testSetLanguage 6", dc.toString(), equalTo(""));
    }

    /**
     * Test of getDisplayName method, of class DCLanguage.
     */
    @Test
    public void testGetDisplayName()
    {
        dc = new DCLanguage(null);
        assertThat("testGetDisplayName 0", dc.getDisplayName(), equalTo("N/A"));

        dc = new DCLanguage("");
        assertThat("testGetDisplayName 1", dc.getDisplayName(), equalTo("N/A"));

        dc = new DCLanguage("other");
        assertThat("testGetDisplayName 2", dc.getDisplayName(),
                equalTo("(Other)"));

        dc = new DCLanguage("en");
        assertThat("testGetDisplayName 3", dc.getDisplayName(),
                equalTo(new Locale("en","").getDisplayName()));

        dc = new DCLanguage("en_GB");
        assertThat("testGetDisplayName 4", dc.getDisplayName(),
                equalTo(new Locale("en","GB").getDisplayName()));
    }
}
