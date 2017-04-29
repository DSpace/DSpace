/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest;

import fi.helsinki.lib.simplerest.TestServlets.GroupServlet;
import org.eclipse.jetty.testing.ServletTester;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author moubarik
 */
public class GroupResourceTest {
    
    private ServletTester tester;
    
    @Before
    public void setUp() throws Exception{
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(GroupServlet.class, "/groups/*");
        tester.start();
    }
    
    @Test
    public void testGetXml(){
        assertEquals(true, true);
    }
    
    @Test
    public void testGetJson(){
        
    }
    
}
