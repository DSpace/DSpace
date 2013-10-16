/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)SimpleRestApplicationTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.restlet.Restlet;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>SimpleRestApplication</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.SimpleRestApplication
 */
public class SimpleRestApplicationTest {

    /**
     * @see fi.helsinki.lib.simplerest.SimpleRestApplication
     */
    private SimpleRestApplication simpleRestApplication;

    public SimpleRestApplicationTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.simpleRestApplication = new SimpleRestApplication();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.simpleRestApplication = null;
    }

    /**
     * Test of createInboundRoot method, of class SimpleRestApplication.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateInboundRoot() {
        Restlet createInboundRoot = this.simpleRestApplication.createInboundRoot();
    }
}
