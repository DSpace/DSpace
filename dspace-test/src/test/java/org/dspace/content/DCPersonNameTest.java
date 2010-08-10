/*
 * DCPersonNameTest.java
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

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests DCPersonName class
 * @author pvillega
 */
public class DCPersonNameTest extends AbstractUnitTest
{

    /**
     * Object to use in the tests
     */
    private DCPersonName dc;


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
        dc = new DCPersonName("");
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
     * Test of DCPersonName constructor, of class DCPersonName.
     */
    @Test
    public void testDCPersonName()
    {
        dc = new DCPersonName();
        assertThat("testDCPersonName 0", dc.getFirstNames(), equalTo(""));
        assertThat("testDCPersonName 1", dc.getLastName(), equalTo(""));
    }

    /**
     * Test of DCPersonName constructor, of class DCPersonName.
     */
    @Test
    public void testDCPersonNameValue()
    {
        dc = new DCPersonName(null);
        assertThat("testDCPersonNameValue 0", dc.getFirstNames(), equalTo(""));
        assertThat("testDCPersonNameValue 1", dc.getLastName(), equalTo(""));

        dc = new DCPersonName("name");
        assertThat("testDCPersonNameValue 2", dc.getFirstNames(), equalTo(""));
        assertThat("testDCPersonNameValue 3", dc.getLastName(), equalTo("name"));

        dc = new DCPersonName("name,firstname");
        assertThat("testDCPersonNameValue 4", dc.getFirstNames(),
                equalTo("firstname"));
        assertThat("testDCPersonNameValue 5", dc.getLastName(), equalTo("name"));

        dc = new DCPersonName("name  ,   firstname");
        assertThat("testDCPersonNameValue 6", dc.getFirstNames(),
                equalTo("firstname"));
        assertThat("testDCPersonNameValue 7", dc.getLastName(), equalTo("name"));
    }

    /**
     * Test of DCPersonName constructor, of class DCPersonName.
     */
    @Test
    public void testDCPersonNameValues()
    {
        dc = new DCPersonName(null, null);
        assertThat("testDCPersonNameValues 0", dc.getFirstNames(), equalTo(""));
        assertThat("testDCPersonNameValues 1", dc.getLastName(), equalTo(""));

        dc = new DCPersonName("name", null);
        assertThat("testDCPersonNameValues 2", dc.getFirstNames(), equalTo(""));
        assertThat("testDCPersonNameValues 3", dc.getLastName(), equalTo("name"));

        dc = new DCPersonName(null, "firstname");
        assertThat("testDCPersonNameValues 4", dc.getFirstNames(),
                equalTo("firstname"));
        assertThat("testDCPersonNameValues 5", dc.getLastName(), equalTo(""));

        dc = new DCPersonName("name","firstname");
        assertThat("testDCPersonNameValues 6", dc.getFirstNames(),
                equalTo("firstname"));
        assertThat("testDCPersonNameValues 7", dc.getLastName(), equalTo("name"));
    }

    /**
     * Test of toString method, of class DCPersonName.
     */
    @Test
    public void testToString()
    {
        dc = new DCPersonName(null, null);
        assertThat("testToString 0", dc.toString(), equalTo(""));

        dc = new DCPersonName("name", null);
        assertThat("testToString 1", dc.toString(), equalTo("name"));

        dc = new DCPersonName(null, "firstname");
        assertThat("testToString 2", dc.toString(), equalTo(""));

        dc = new DCPersonName("name","firstname");
        assertThat("testToString 3", dc.toString(), equalTo("name, firstname"));
    }

    /**
     * Test of getFirstNames method, of class DCPersonName.
     */
    @Test
    public void testGetFirstNames()
    {
         dc = new DCPersonName(null, null);
        assertThat("testGetFirstNames 0", dc.getFirstNames(), equalTo(""));

        dc = new DCPersonName("name", null);
        assertThat("testGetFirstNames 1", dc.getFirstNames(), equalTo(""));

        dc = new DCPersonName(null, "firstname");
        assertThat("testGetFirstNames 2", dc.getFirstNames(),
                equalTo("firstname"));

        dc = new DCPersonName("name","firstname");
        assertThat("testGetFirstNames 3", dc.getFirstNames(),
                equalTo("firstname"));
    }

    /**
     * Test of getLastName method, of class DCPersonName.
     */
    @Test
    public void testGetLastName()
    {
        dc = new DCPersonName(null, null);
        assertThat("testGetLastName 0", dc.getLastName(), equalTo(""));

        dc = new DCPersonName("name", null);
        assertThat("testGetLastName 1", dc.getLastName(), equalTo("name"));

        dc = new DCPersonName(null, "firstname");
        assertThat("testGetLastName 2", dc.getLastName(), equalTo(""));

        dc = new DCPersonName("name","firstname");
        assertThat("testGetLastName 3", dc.getLastName(), equalTo("name"));
    }

}
