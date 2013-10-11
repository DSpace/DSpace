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
 * @(#)MetadataSchemasResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

import fi.helsinki.lib.simplerest.MetadataSchemasResource;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>MetadataSchemasResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.MetadataSchemasResource
 */
public class MetadataSchemasResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.MetadataSchemasResource
     */
    private MetadataSchemasResource metadataSchemasResource;

    public MetadataSchemasResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.metadataSchemasResource = new MetadataSchemasResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.metadataSchemasResource = null;
    }

    /**
     * Test of relativeUrl method, of class MetadataSchemasResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = MetadataSchemasResource.relativeUrl(44);
        assertEquals("metadataschemas", actualUrl);
    }

    /**
     * Test of toXml method, of class MetadataSchemasResource.
     */
    @Test
    public void testToXml() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataSchemasResource.toXml();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.sql.SQLException: java.lang.RuntimeException: "
                     + "Cannot find dspace.cfg",
                     representation.getText());
    }

    /**
     * Test of put method, of class MetadataSchemasResource.
     */
    @Test
    public void testPut() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataSchemasResource.put(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Metadata schemas resource does not allow PUT method.",
                     representation.getText());
    }

    /**
     * Test of addCommunity method, of class MetadataSchemasResource.
     */
    @Test(expected = NullPointerException.class)
    public void testAddCommunity() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataSchemasResource.addCommunity(null);
    }

    /**
     * Test of delete method, of class MetadataSchemasResource.
     */
    @Test
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataSchemasResource.delete();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Metadata schemas resource does not allow DELETE method.",
                     representation.getText());
    }
}
