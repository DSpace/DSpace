/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
