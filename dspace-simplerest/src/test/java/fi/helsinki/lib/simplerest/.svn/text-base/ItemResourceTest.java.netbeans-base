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
 * @(#)ItemResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

import fi.helsinki.lib.simplerest.ItemResource;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>ItemResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.ItemResource
 */
public class ItemResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.ItemResource
     */
    private ItemResource itemResource;

    public ItemResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.itemResource = new ItemResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.itemResource = null;
    }

    /**
     * Test of relativeUrl method, of class ItemResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = ItemResource.relativeUrl(11);
        assertEquals("item/11", actualUrl);
    }

    /**
     * Test of doInit method, of class ItemResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.itemResource.doInit();
    }

    /**
     * Test of toXml method, of class ItemResource.
     */
    @Test
    public void testToXml() {
        StringRepresentation representation =
                             (StringRepresentation) this.itemResource.toXml();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.sql.SQLException: java.lang.RuntimeException: "
                     + "Cannot find dspace.cfg",
                     representation.getText());
    }

    /**
     * Test of editItem method, of class ItemResource.
     */
    @Test
    public void testEditItem() {
        StringRepresentation representation =
                             (StringRepresentation) this.itemResource.editItem(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }

    /**
     * Test of addBundle method, of class ItemResource.
     */
    @Test
    public void testAddBundle() {
        StringRepresentation representation =
                             (StringRepresentation) this.itemResource.addBundle(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }

    /**
     * Test of deleteItem method, of class ItemResource.
     */
    @Test
    public void testDeleteItem() {
        StringRepresentation representation =
                             (StringRepresentation) this.itemResource.deleteItem();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }
}
