/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dspace.statistics.util.IPTable.IPFormatException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class IPTableTest {
    private static final String LOCALHOST = "127.0.0.1";

    public IPTableTest() {
    }

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
     * Test of add method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testAdd() throws Exception {
    }

    /**
     * Test of contains method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testContains()
            throws Exception {
        IPTable instance = new IPTable();
        instance.add(LOCALHOST);
        boolean contains;

        contains = instance.contains(LOCALHOST);
        assertTrue("Address that was add()ed should match", contains);

        contains = instance.contains("192.168.1.1");
        assertFalse("Address that was not add()ed should not match", contains);

        contains = instance.contains("fec0:0:0:1::2");
        assertFalse("IPv6 address should not match anything.", contains);
    }

    /**
     * Test of contains method when presented with an invalid address.
     * @throws Exception passed through.
     */
    @Test(expected = IPFormatException.class)
    public void testContainsBadFormat()
            throws Exception {
        IPTable instance = new IPTable();
        instance.add(LOCALHOST);
        boolean contains;

        // This should throw an IPFormatException.
        contains = instance.contains("axolotl");
        assertFalse("Nonsense string should raise an exception.", contains);
    }

    /**
     * Test of toSet method, of class IPTable.
     */
    @Ignore
    @Test
    public void testToSet() {
    }
}
