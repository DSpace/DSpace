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
 * @(#)BitstreamResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.BitstreamServlet;
import fi.helsinki.lib.simplerest.stubs.StubBitstream;
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
 * Testing the methods of <code>BitstreamResource</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.BitstreamResource
 */
public class BitstreamResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.BitstreamResource
     */
    private BitstreamResource bitstreamResource;
    private ServletTester tester;

    public BitstreamResourceTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     */
    @Before
    public void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(BitstreamServlet.class, "/bitstream/*");
        tester.start();
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/bitstream/xml");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        
        String response = resp.getContent();
        
        assertEquals(200, resp.getStatus());
        String[] attributes = {"name", "mimetype", "description", "userformatdescription", "source", "sequenceid", "sizebytes"};
        for(String attribute : attributes){
            assertEquals(response.contains(attribute), true);
        }
        
        String[] values = {"testi.pdf", "application/pdfs", "1337"};
        for(String value : values){
            assertEquals(response.contains(value), true);
        }
    }
    
    @Test
    public void testGetJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/bitstream/json");
        resp.parse(tester.getResponses(req.generate()));
        
        Gson gson = new Gson();
        
        System.out.println(resp.getContent());
        StubBitstream sb = gson.fromJson(resp.getContent(), StubBitstream.class);
        
        assertEquals(200, resp.getStatus());
        assertEquals(sb.getId(), 1);
        assertEquals(sb.getDescription(), "");
        assertEquals(sb.getName(), "testi.pdf");
        assertEquals(sb.getSizebytes().compareTo(1337L), 0);
        assertEquals(sb.getUserformatdescription(), "");
        assertEquals(sb.getMimetype(), "application/pdfs");
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.bitstreamResource = null;
    }

    /**
     * Test of relativeUrl method, of class BitstreamResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = BitstreamResource.relativeUrl(5);
        assertEquals("bitstream/5", actualUrl);
    }

}
