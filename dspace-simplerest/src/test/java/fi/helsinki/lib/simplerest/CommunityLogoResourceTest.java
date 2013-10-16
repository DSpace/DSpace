/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)CommunityLogoResourceTest.java
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
public class CommunityLogoResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.CommunityLogoResource
     */
    private CommunityLogoResource communityLogoResource;

    public CommunityLogoResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() {
        this.communityLogoResource = new CommunityLogoResource();
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.communityLogoResource = null;
    }

    /**
     * Test of relativeUrl method, of class CommunityLogoResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = CommunityLogoResource.relativeUrl(76);
        assertEquals("community/76/logo", actualUrl);
    }

    /**
     * Test of doInit method, of class CommunityLogoResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.communityLogoResource.doInit();
    }
    
    /**
     * Test of put method, of class CommunityLogoResource.
     */
    @Test(expected = NullPointerException.class)
    public void testPut() {
        StringRepresentation representation =
                             (StringRepresentation) this.communityLogoResource.put(null);
    }

    /**
     * Test of post method, of class CommunityLogoResource.
     */
    @Test
    public void testPost() {
        StringRepresentation representation =
                             (StringRepresentation) this.communityLogoResource.post(null);
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("Community logo resource does not allow POST method.",
                     representation.getText());
    }

    /**
     * Test of delete method, of class CommunityLogoResource.
     */
    @Test
    public void testDelete() {
        StringRepresentation representation =
                             (StringRepresentation) this.communityLogoResource.delete();
        assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
        assertEquals("java.lang.NullPointerException", representation.getText());
    }
}
