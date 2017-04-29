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
 * @(#)BundleResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>BundleResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.BundleResource
 */
public class BundleResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.BundleResource
     */
    private BundleResource bundleResource;

    public BundleResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.bundleResource = new BundleResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.bundleResource = null;
    }

    /**
     * Test of relativeUrl method, of class BundleResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = BundleResource.relativeUrl(6);
        assertEquals("bundle/6", actualUrl);

    }

    /**
     * Test of doInit method, of class BundleResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.bundleResource.doInit();
    }

    /**
     * Test of editBundle method, of class BundleResource.
     */
    @Test(expected = NullPointerException.class)
    public void testEditBundle() {
        this.bundleResource.editBundle(null);
    }

    /**
     * Test of addBitstream method, of class BundleResource.
     */
    @Test
    public void testAddBitstream() {
        StringRepresentation representation =
                             (StringRepresentation) this.bundleResource.addBitstream(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }

    /**
     * Test of deleteBundle method, of class BundleResource.
     */
    @Test
    public void testDeleteBundle() {
        StringRepresentation representation =
                             (StringRepresentation) this.bundleResource.deleteBundle();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }
}
