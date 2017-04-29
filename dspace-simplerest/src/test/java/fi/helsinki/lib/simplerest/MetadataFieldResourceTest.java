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
 * @(#)MetadataFieldResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.MetadataServlet;
import fi.helsinki.lib.simplerest.stubs.StubMetadata;
import java.io.IOException;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
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
 * Testing the methods of <code>MetadataFieldResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.MetadataFieldResource
 */
public class MetadataFieldResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.MetadataFieldResource
     */
    private ServletTester tester;

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(MetadataServlet.class, "/metadatafield/*");
        tester.start();
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/metadatafield/xml");
        resp.parse(tester.getResponses(req.generate()));
        
        String content = resp.getContent();
        
        assertEquals(200, resp.getStatus());
        String[] attributes = {"schema", "element", "qualifier", "scopenote"};
        for(String attribute : attributes){
            assertEquals(content.contains(attribute), true);
        }
        
        String[] values = {"dckk", "testElement", "testQualifier", "Description"};
        for(String value : values){
            assertEquals(content.contains(value), true);
        }
    }
    
    @Test
    public void testJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/metadatafield/json");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        Gson gson = new Gson();
        
        StubMetadata sm = gson.fromJson(resp.getContent(), StubMetadata.class);
        
        assertEquals(200, resp.getStatus());
        assertEquals(sm.getElement(), "testElement");
        assertEquals(sm.getId(), 1);
        assertEquals(sm.getSchema(), "dckk");
        assertEquals(sm.getQualifier(), "testQualifier");
        assertEquals(sm.getScopeNote(), "Description");
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() throws Exception {
        tester.stop();
    }

    /**
     * Test of relativeUrl method, of class MetadataFieldResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = MetadataFieldResource.relativeUrl(73);
        assertEquals("metadatafield/73", actualUrl);
    }
}
