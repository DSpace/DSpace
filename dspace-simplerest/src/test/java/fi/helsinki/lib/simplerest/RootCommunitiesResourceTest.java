/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2013 National Library of Finland
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
 * @(#)RootCommunitiesResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.RootCommunitiesServlet;
import fi.helsinki.lib.simplerest.stubs.StubCommunity;
import java.io.IOException;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;



/**
 * JUnit tests.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>RootCommunitiesResource</code> class.
 * @author Anis Moubarik
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.RootCommunitiesResource
 */
public class RootCommunitiesResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.RootCommunitiesResource
     */
    private RootCommunitiesResource rootCommunitiesResource;
    
    private ServletTester tester;

    public RootCommunitiesResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(RootCommunitiesServlet.class, "/rootcommunities/*");
        tester.start();
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/rootcommunities/xml");
        resp.parse(tester.getResponses(req.generate()));
        
        System.out.println(resp.getContent());
        
        assertEquals(200, resp.getStatus());
        assertEquals(resp.getContent().contains("test"), true);
        assertEquals(resp.getContent().contains("Root"), true);
        assertEquals(resp.getContent().contains("community/1"), true);
    }
    
    @Test
    public void testGetJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/rootcommunities/json");
        resp.parse(tester.getResponses(req.generate()));
        
        System.out.println(resp.getContent());
        
        assertEquals(200, resp.getStatus());
        Gson gson = new Gson();
        
        StubCommunity[] communities = gson.fromJson(resp.getContent(), StubCommunity[].class);
        
        assertEquals(communities.length, 2);
        assertEquals(communities[0].getId(), 1);
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.rootCommunitiesResource = null;
    }

    /**
     * Test of relativeUrl method, of class RootCommunitiesResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = RootCommunitiesResource.relativeUrl(1543);
        assertEquals("rootcommunities", actualUrl);
    }
}
