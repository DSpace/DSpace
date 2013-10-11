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
 * @(#)MetadataSchemaResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.SchemaServlet;
import fi.helsinki.lib.simplerest.stubs.StubSchema;
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
 * Testing the methods of <code>MetadataSchemaResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.MetadataSchemaResource
 */
public class MetadataSchemaResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.MetadataSchemaResource
     */
    private ServletTester tester;

    public MetadataSchemaResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(SchemaServlet.class, "/metadataschema/*");
        tester.start();
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/metadataschema/xml");
        resp.parse(tester.getResponses(req.generate()));
        
        String content = resp.getContent();
        
        assertEquals(200, resp.getStatus());
        String[] attributes = {"name", "namespace"};
        for(String attribute : attributes){
            assertEquals(content.contains(attribute), true);
        }
        
        String[] values = {"dckk", "http://kk.fi/dckk/"};
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
        req.setURI("/metadataschema/json");
        resp.parse(tester.getResponses(req.generate()));
        Gson gson = new Gson();
        
        StubSchema ss = gson.fromJson(resp.getContent(), StubSchema.class);
        
        assertEquals(200, resp.getStatus());
        assertEquals(ss.getId(), 1);
        assertEquals(ss.getName(), "dckk");
        assertEquals(ss.getNamespace(), "http://kk.fi/dckk/");
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
     * Test of relativeUrl method, of class MetadataSchemaResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = MetadataSchemaResource.relativeUrl(89);
        assertEquals("metadataschema/89", actualUrl);
    }
}
