/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;


/**
 * Tests DCLanguageTest class
 * @author pvillega
 */
public class DCLanguageTest
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
    public void init()
    {
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
    public void destroy()
    {
        dc = null;
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
