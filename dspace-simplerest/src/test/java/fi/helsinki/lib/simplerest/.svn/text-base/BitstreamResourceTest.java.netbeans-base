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
 * @(#)BitstreamResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

import fi.helsinki.lib.simplerest.BitstreamResource;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>BitstreamResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.BitstreamResource
 */
public class BitstreamResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.BitstreamResource
     */
    private BitstreamResource bitstreamResource;

    public BitstreamResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.bitstreamResource = new BitstreamResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.bitstreamResource = null;
    }

    /**
     * Test of relativeUrl method, of class BitstreamResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = BitstreamResource.relativeUrl(5);
        assertEquals("bitstream/5", actualUrl);
    }

    /**
     * Test of doInit method, of class BitstreamResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() {
        this.bitstreamResource.doInit();
    }

    /**
     * Test of get method, of class BitstreamResource.
     */
    @Test
    public void testGet() {
        StringRepresentation representation =
                             (StringRepresentation) this.bitstreamResource.get();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("SQLException", representation.getText());
    }

    /**
     * Test of put method, of class BitstreamResource.
     */
    @Test(expected = NullPointerException.class)
    public void testPut() {
        StringRepresentation representation =
                             (StringRepresentation) this.bitstreamResource.put(null);
    }

    /**
     * Test of post method, of class BitstreamResource.
     */
    @Test
    public void testPost() {
        StringRepresentation representation =
                             (StringRepresentation) this.bitstreamResource.post(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Bitstream resource does not allow POST method.",
                     representation.getText());
    }

    /**
     * Test of delete method, of class BitstreamResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.bitstreamResource.delete();
    }
}
