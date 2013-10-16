/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)LaxMapVerifierTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>LaxMapVerifier</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.LaxMapVerifier
 */
public class LaxMapVerifierTest {

    /**
     * @see org.restlet.Request
     */
    private Request request;
    /**
     * @see org.restlet.Response
     */
    private Response response;
    /**
     * @see fi.helsinki.lib.simplerest.LaxMapVerifier
     */
    private LaxMapVerifier laxMapVerifier;

    public LaxMapVerifierTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.request = new Request();
        this.response = new Response(this.request);
        this.laxMapVerifier = new LaxMapVerifier();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.laxMapVerifier = null;
    }

    /**
     * Test of verify method, of class LaxMapVerifier.
     */
    @Test
    public void testVerifyIfMethodGET() {
        this.request.setMethod(Method.GET);
        int actual = this.laxMapVerifier.verify(this.request, this.response);
        assertEquals(4, actual);
    }

    /**
     * Test of verify method, of class LaxMapVerifier.
     */
    @Test
    public void testVerifyElse() {
        this.request.setMethod(Method.HEAD);
        int actual = this.laxMapVerifier.verify(this.request, this.response);
        assertEquals(0, actual);
    }
}
