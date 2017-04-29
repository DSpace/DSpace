/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2013 National Library of Finland
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
