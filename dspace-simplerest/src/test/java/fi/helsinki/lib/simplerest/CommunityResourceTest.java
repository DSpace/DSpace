/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)CommunityResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.CommunityServlet;
import fi.helsinki.lib.simplerest.stubs.StubCommunity;
import java.io.IOException;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>CommunityResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.CommunityResource
 */
public class CommunityResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.CommunityResource
     */
    private CommunityResource communityResource;
    
    private ServletTester tester;

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(CommunityServlet.class, "/community/*");
        tester.addServlet("org.mortaby.jetty.servlet.DefaultServlet", "/");
        tester.start();
        
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.communityResource = null;
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/community/xml");
        resp.parse(tester.getResponses(req.generate()));
        
        System.out.println(resp.getContent());
        assertEquals(200, resp.getStatus());
        String[] attributes = { "short_description", "introductory_text",
                                "copyright_text", "side_bar_text" };        
        for(String attribute : attributes){
            assertEquals(resp.getContent().contains(attribute), true);
        }
    }
    
    @Test
    public void testGetJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/community/json");
        resp.parse(tester.getResponses(req.generate()));
        Gson gson = new Gson();
        System.out.println(resp.getContent());
        StubCommunity sc = gson.fromJson(resp.getContent(), StubCommunity.class);
        assertEquals(200, resp.getStatus());
        assertEquals(sc.getId(), 1);
        assertEquals(sc.getName(), "test");
        assertEquals(sc.getCopyright_text(), "testi copyright");
        assertEquals(sc.getIntroductory_text(), "testi intro");
        assertEquals(sc.getShort_description(), "testi kuvaus");
        assertEquals(sc.getSide_bar_text(), "testi sidebar");
    }

    /**
     * Test of relativeUrl method, of class CommunityResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = CommunityResource.relativeUrl(23);
        assertEquals("community/23", actualUrl);
    }

    /**
     * Test of doInit method, of class CommunityResource.
     */
    @Test(expected = NullPointerException.class)
    public void testDoInit() throws Exception {
        this.communityResource.doInit();
    }

    /**
     * Test of edit method, of class CommunityResource.
     */
    @Test
    public void testEdit() throws IOException, Exception {
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("PUT");
        req.setHeader("HOST", "tester");
        req.setURI("/community/edit");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        assertEquals(200, resp.getStatus());
        String[] attributes = { "short_description", "introductory_text",
                                "copyright_text", "side_bar_text" };        
        for(String attribute : attributes){
            assertEquals(resp.getContent().contains(attribute), true);
        }
    }

    /**
     * Test of delete method, of class CommunityResource.
     */
//    @Test
//    public void testDelete() {
//        //StringRepresentation representation = (StringRepresentation) this.communityResource.delete();
//        //assertEquals(MediaType.TEXT_PLAIN, representation.getMediaType());
//        //assertEquals("java.lang.NullPointerException", representation.getText());
//    }
}
