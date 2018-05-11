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
 * Tests DCPersonName class
 * @author pvillega
 */
public class DCPersonNameTest
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
    public void init()
    {
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
    public void destroy()
    {
        dc = null;
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
