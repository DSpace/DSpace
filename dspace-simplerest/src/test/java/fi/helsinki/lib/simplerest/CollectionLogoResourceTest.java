/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
