/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)MetadataFieldsResourceTest.java
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
 * Testing the methods of <code>MetadataFieldsResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.MetadataFieldsResource
 */
public class MetadataFieldsResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.MetadataFieldsResource
     */
    private MetadataFieldsResource metadataFieldsResource;

    public MetadataFieldsResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.metadataFieldsResource = new MetadataFieldsResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.metadataFieldsResource = null;
    }

    /**
     * Test of relativeUrl method, of class MetadataFieldsResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = MetadataFieldsResource.relativeUrl(0);
        assertEquals("metadatafields", actualUrl);
    }

    /**
     * Test of put method, of class MetadataFieldsResource.
     */
    @Test
    public void testPut() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataFieldsResource.put(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Metadata fields resource does not allow PUT method.",
                     representation.getText());
    }

    /**
     * Test of addCommunity method, of class MetadataFieldsResource.
     */
    @Test(expected = NullPointerException.class)
    public void testAddCommunity() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataFieldsResource.addCommunity(null);
    }

    /**
     * Test of delete method, of class MetadataFieldsResource.
     */
    @Test
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.metadataFieldsResource.delete();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Metadata fields resource does not allow DELETE method.",
                     representation.getText());
    }
}
