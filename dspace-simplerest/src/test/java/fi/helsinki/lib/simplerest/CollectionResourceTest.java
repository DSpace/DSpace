/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)CollectionResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.representation.StringRepresentation;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>CollectionResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.CollectionResource
 */
public class CollectionResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.CollectionResource
     */
    private CollectionResource collectionResource;

    public CollectionResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.collectionResource = new CollectionResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.collectionResource = null;
    }

    /**
     * Test of relativeUrl method, of class CollectionResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = CollectionResource.relativeUrl(3);
        assertEquals("collection/3", actualUrl);
    }

    /**
     * Test of doInit method, of class CollectionResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.collectionResource.doInit();
    }


    /**
     * Test of edit method, of class CollectionResource.
     */
    @Test(expected = NullPointerException.class)
    public void testEdit() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionResource.edit(null);
    }

    /**
     * Test of delete method, of class CollectionResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.collectionResource.delete();
    }
}
