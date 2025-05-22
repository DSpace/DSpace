/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.jupiter.api.Assertions.assertEquals;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tim
 */
public class IndexVersionTest {

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
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
        assertEquals(-1, IndexVersion.compareSoftwareVersions("5", "6"));
        assertEquals(-1, IndexVersion.compareSoftwareVersions("4.1", "6"));
        assertEquals(-1, IndexVersion.compareSoftwareVersions("4.1", "4.4"));
        assertEquals(-1, IndexVersion.compareSoftwareVersions("4.1", "4.10"));
        assertEquals(-1, IndexVersion.compareSoftwareVersions("4.4", "4.10"));
        assertEquals(-1, IndexVersion.compareSoftwareVersions("4.4", "5.1"));

        // greater than tests (return 1)
        assertEquals(1, IndexVersion.compareSoftwareVersions("6", "5"));
        assertEquals(1, IndexVersion.compareSoftwareVersions("6.10", "6.4"));
        assertEquals(1, IndexVersion.compareSoftwareVersions("6.10", "6.1"));
        assertEquals(1, IndexVersion.compareSoftwareVersions("5.3", "2.4"));

        // equality tests (return 0)
        assertEquals(0, IndexVersion.compareSoftwareVersions("5", "5.0"));
        assertEquals(0, IndexVersion.compareSoftwareVersions("6", "6"));
        assertEquals(0, IndexVersion.compareSoftwareVersions("4.2", "4.2"));
        // we ignore subminor versions, so these should be "equal"
        assertEquals(0, IndexVersion.compareSoftwareVersions("4.2.1", "4.2"));
    }

}
