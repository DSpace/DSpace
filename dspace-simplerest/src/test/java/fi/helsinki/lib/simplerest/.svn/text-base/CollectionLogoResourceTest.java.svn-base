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
 * @(#)CollectionLogoResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

import fi.helsinki.lib.simplerest.CollectionLogoResource;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>CollectionLogoResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.CollectionLogoResource
 */
public class CollectionLogoResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.CollectionLogoResource
     */
    private CollectionLogoResource collectionLogoResource;

    public CollectionLogoResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.collectionLogoResource = new CollectionLogoResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.collectionLogoResource = null;
    }

    /**
     * Test of relativeUrl method, of class CollectionLogoResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = CollectionLogoResource.relativeUrl(4);
        assertEquals("collection/4/logo", actualUrl);
    }

    /**
     * Test of doInit method, of class CollectionLogoResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.collectionLogoResource.doInit();
    }

    /**
     * Test of get method, of class CollectionLogoResource.
     */
    @Test
    public void testGet() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionLogoResource.get();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.sql.SQLException: java.lang.RuntimeException: "
                     + "Cannot find dspace.cfg", representation.getText());
    }

    /**
     * Test of put method, of class CollectionLogoResource.
     */
    @Test(expected = NullPointerException.class)
    public void testPut() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionLogoResource.put(null);
    }

    /**
     * Test of post method, of class CollectionLogoResource.
     */
    @Test
    public void testPost() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionLogoResource.post(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Collection logo resource does not allow POST method.",
                     representation.getText());
    }

    /**
     * Test of delete method, of class CollectionLogoResource.
     */
    @Test
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionLogoResource.delete();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }
}
