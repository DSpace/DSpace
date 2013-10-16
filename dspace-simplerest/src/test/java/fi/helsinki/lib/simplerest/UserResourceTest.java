/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.TestServlets.UserServlet;
import fi.helsinki.lib.simplerest.stubs.StubUser;
import java.io.IOException;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author moubarik
 */
public class UserResourceTest {
        
    private ServletTester tester;
    
    @Before
    public void setUp() throws Exception{
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(UserServlet.class, "/user/*");
        tester.start();
    }
    
    @Test
    public void testGetXml() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/user/xml");
        resp.parse(tester.getResponses(req.generate()));
        System.out.println(resp.getContent());
        
        assertEquals(200, resp.getStatus());
        String[] attributes = {"email", "id", "language", "netid", "fullname", "firstname",
        "lastname", "can login", "require certificate", "self registered"};
        for(String attribute : attributes){
            assertEquals(resp.getContent().contains(attribute), true);
        }
        
        String[] values = {"test(a)test.com", "fi", "1", "testi testaaja", "testi", "testaaja", "false", "true"};
        
        for(String value : values){
            assertEquals(resp.getContent().contains(value), true);
        }
    }
    
    @Test
    public void testGetJson() throws IOException, Exception{
        HttpTester req = new HttpTester();
        HttpTester resp = new HttpTester();
        
        req.setMethod("GET");
        req.setHeader("HOST", "tester");
        req.setURI("/user/json");
        resp.parse(tester.getResponses(req.generate()));
        
        System.out.println(resp.getContent());
        
        Gson gson = new Gson();
        StubUser su = gson.fromJson(resp.getContent(), StubUser.class);
        
        assertEquals(200, resp.getStatus());
        assertEquals(su.getId(), 1);
        assertEquals(su.getEmail(), "test@test.com");
        assertEquals(su.getLanguage(), "fi");
        assertEquals(su.getNetid(), "1");
        assertEquals(su.getFullname(), "testi testaaja");
        assertEquals(su.getFirstname(), "testi");
        assertEquals(su.getLastname(), "testaaja");
        assertEquals(su.isCan_login(), true);
        assertEquals(su.isRequire_certificate(), false);
        assertEquals(su.isSelf_registered(), true);
    }
    
}
