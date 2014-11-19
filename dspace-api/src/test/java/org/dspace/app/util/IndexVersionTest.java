/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Tim
 */
public class IndexVersionTest {
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of compareSoftwareVersions method, of class IndexVersion.
     */
    @Test
    public void testCompareSoftwareVersions() throws Exception {
        
        // Test various version comparisons. Remember, in software versions:
        // 4.1 < 4.4 < 4.5 < 4.10 < 4.21 < 4.51
        
        // less than tests (return -1)
        assertEquals(IndexVersion.compareSoftwareVersions("5", "6"), -1);
        assertEquals(IndexVersion.compareSoftwareVersions("4.1", "6"), -1);
        assertEquals(IndexVersion.compareSoftwareVersions("4.1", "4.4"), -1);
        assertEquals(IndexVersion.compareSoftwareVersions("4.1", "4.10"), -1);
        assertEquals(IndexVersion.compareSoftwareVersions("4.4", "4.10"), -1);
        assertEquals(IndexVersion.compareSoftwareVersions("4.4", "5.1"), -1);
        
        // greater than tests (return 1)
        assertEquals(IndexVersion.compareSoftwareVersions("6", "5"), 1);
        assertEquals(IndexVersion.compareSoftwareVersions("6.10", "6.4"), 1);
        assertEquals(IndexVersion.compareSoftwareVersions("6.10", "6.1"), 1);
        assertEquals(IndexVersion.compareSoftwareVersions("5.3", "2.4"), 1);
        
        // equality tests (return 0)
        assertEquals(IndexVersion.compareSoftwareVersions("5", "5.0"), 0);
        assertEquals(IndexVersion.compareSoftwareVersions("6", "6"), 0);
        assertEquals(IndexVersion.compareSoftwareVersions("4.2", "4.2"), 0);
        // we ignore subminor versions, so these should be "equal"
        assertEquals(IndexVersion.compareSoftwareVersions("4.2.1", "4.2"), 0);
    }
    
}
