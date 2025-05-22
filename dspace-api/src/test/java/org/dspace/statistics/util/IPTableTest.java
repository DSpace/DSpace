/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.dspace.statistics.util.IPTable.IPFormatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mwood
 */
public class IPTableTest {
    private static final String LOCALHOST = "127.0.0.1";

    public IPTableTest() {
    }

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
     * Test of add method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testAdd() throws Exception {
        IPTable instance = new IPTable();
        // Add IP address
        instance.add(LOCALHOST);
        // Add IP range (contains 256 addresses)
        instance.add("192.168.1");

        // Make sure it returns the addresses for all ranges
        Set<String> ipSet = instance.toSet();
        assertEquals(257, ipSet.size());
        assertTrue(ipSet.contains(LOCALHOST));
        assertTrue(ipSet.contains("192.168.1.0"));
        assertTrue(ipSet.contains("192.168.1.255"));
    }

    @Test
    public void testAddSameIPTwice() throws Exception {
        IPTable instance = new IPTable();
        // Add same IP twice
        instance.add(LOCALHOST);
        instance.add(LOCALHOST);
        // Verify it only exists once
        assertEquals(1, instance.toSet().size());

        instance = new IPTable();
        // Add IP range w/ 256 addresses & then add an IP from within that range
        instance.add("192.168.1");
        instance.add("192.168.1.1");
        // Verify only the range exists
        Set<String> ipSet = instance.toSet();
        assertEquals(256, ipSet.size());
        assertTrue(ipSet.contains("192.168.1.1"));

        instance = new IPTable();
        // Now, switch order. Add IP address, then add a range encompassing that IP
        instance.add("192.168.1.1");
        instance.add("192.168.1");
        // Verify only the range exists
        ipSet = instance.toSet();
        assertEquals(256, ipSet.size());
        assertTrue(ipSet.contains("192.168.1.1"));
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
        assertTrue(contains, "Address that was add()ed should match");

        contains = instance.contains("192.168.1.1");
        assertFalse(contains, "Address that was not add()ed should not match");

        contains = instance.contains("fec0:0:0:1::2");
        assertFalse(contains, "IPv6 address should not match anything.");

        // Now test contains() finds an IP within a range of IPs
        instance.add("192.168.1");
        contains = instance.contains("192.168.1.1");
        assertTrue(contains, "IP within an add()ed range should match");
    }

    @Test
    public void testDashRangeContains() throws Exception {
        IPTable instance = new IPTable();
        instance.add("192.168.0.0 - 192.168.0.245");

        assertTrue(instance.contains("192.168.0.0"), "Range should contain lower limit");
        assertTrue(instance.contains("192.168.0.245"), "Range should contain upper limit");
        assertTrue(instance.contains("192.168.0.123"), "Range should contain value in between limits");
        assertTrue(instance.contains("192.168.0.234"), "Range should contain value in between limits");

        assertFalse(instance.contains("192.167.255.255"), "Range should not contain value below lower limit");
        assertFalse(instance.contains("192.168.0.246"), "Range should not contain value above upper limit");
    }

    @Test
    public void testSubnetRangeContains() throws Exception {
        IPTable instance = new IPTable();
        instance.add("192.168.0.0/30");  // translates to 192.168.0.0 - 192.168.0.3

        assertTrue(instance.contains("192.168.0.0"), "Range should contain lower limit");
        assertTrue(instance.contains("192.168.0.3"), "Range should contain upper limit");
        assertTrue(instance.contains("192.168.0.1"), "Range should contain values in between limits");
        assertTrue(instance.contains("192.168.0.2"), "Range should contain values in between limits");

        assertFalse(instance.contains("192.167.255.255"), "Range should not contain value below lower limit");
        assertFalse(instance.contains("192.168.0.4"), "Range should not contain value above upper limit");
    }

    @Test
    public void testImplicitRangeContains() throws Exception {
        IPTable instance = new IPTable();
        instance.add("192.168.1");

        assertTrue(instance.contains("192.168.1.0"), "Range should contain lower limit");
        assertTrue(instance.contains("192.168.1.255"), "Range should contain upper limit");
        assertTrue(instance.contains("192.168.1.123"), "Range should contain values in between limits");
        assertTrue(instance.contains("192.168.1.234"), "Range should contain values in between limits");

        assertFalse(instance.contains("192.168.0.0"), "Range should not contain value below lower limit");
        assertFalse(instance.contains("192.168.2.0"), "Range should not contain value above upper limit");
    }

    /**
     * Test of isEmpty method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testisEmpty() throws Exception {
        IPTable instance = new IPTable();
        assertTrue(instance.isEmpty());
        instance.add(LOCALHOST);
        assertFalse(instance.isEmpty());
    }

    /**
     * Test of contains method when presented with an invalid address.
     * @throws Exception passed through.
     */
    @Test
    public void testContainsBadFormat() {
        assertThrows(IPFormatException.class, () -> {
            IPTable instance = new IPTable();
            instance.add(LOCALHOST);
            boolean contains;

            // This should throw an IPFormatException.
            contains = instance.contains("axolotl");
            assertFalse(contains, "Nonsense string should raise an exception.");
        });
    }

    /**
     * Test of toSet method, of class IPTable.
     */
    @Disabled
    @Test
    public void testToSet() {
    }
}
