/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2011 National Library of Finland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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

import fi.helsinki.lib.simplerest.LaxMapVerifier;

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
