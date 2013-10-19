/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
