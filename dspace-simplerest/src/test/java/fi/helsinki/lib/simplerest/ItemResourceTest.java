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
 * @(#)ItemResourceTest.java
 */
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.ItemServlet;
import fi.helsinki.lib.simplerest.stubs.StubItem;
import java.io.IOException;
import org.dspace.content.DCValue;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;


/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>ItemResource</code> class.
 * @author Anis Moubarik
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.ItemResource
 */
public class ItemResourceTest {

    /**
     * @see fi.helsinki.lib.simplerest.ItemResource
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
        tester.addServlet(ItemServlet.class, "/item/*");
        tester.start();
    }


    /**
     * Test of relativeUrl method, of class ItemResource.
     */
    @Test
    public void testRelativeUrl() {
        String actualUrl = ItemResource.relativeUrl(11);
        assertEquals("item/11", actualUrl);
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/item/xml");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        
        assertEquals(200, resp.getStatus());
        assertEquals(resp.getContent().contains("dc.contributor.author"), true);
        assertEquals(resp.getContent().contains("dc.date.issued"), true);
    }
    
    @Test
    public void testGetJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/item/json");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        Gson gson = new Gson();
        StubItem si = gson.fromJson(resp.getContent(), StubItem.class);
        
        assertEquals(200, resp.getStatus());
        assertEquals(si.in_archive(), true);
        assertEquals(si.getOwningCollectionID(), 0);
        assertEquals(si.withdrawn(), false);
        assertEquals(si.getId(), 1);
        
        DCValue[] metadata = si.getMetadata();
        assertEquals(metadata[0].schema, "dc");
        assertEquals(metadata[0].element, "contributor");
        assertEquals(metadata[0].qualifier, "author");
        assertEquals(metadata[0].value, "Testi Testaaja");
        
        assertEquals(metadata[1].schema, "dc");
        assertEquals(metadata[1].element, "date");
        assertEquals(metadata[1].qualifier, "issued");
        assertEquals(metadata[1].value, "2013");
    }
    
    @Test
    public void testEdit() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("PUT");
        req.setHeader("HOST", "tester");
        req.setURI("/item/edit");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        assertEquals(200, resp.getStatus());
        assertEquals(resp.getContent().contains("dc.contributor.author"), true);
        assertEquals(resp.getContent().contains("dc.date.issued"), true);
    }
}
