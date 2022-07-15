/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 *
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Mark Wood
 * @author Ben Bosman
 * @author Roeland Dillen
 */
public class IPMatcherTest {
    private static final String IP6_FULL_ADDRESS1 = "2001:18e8:3:171:218:8bff:fe2a:56a4";
    private static final String IP6_FULL_ADDRESS2 = "2001:18e8:3:171:218:8bff:fe2a:56a3";
    private static final String IP6_MASKED_ADDRESS = "2001:18e8:3::/48";

    private final static int increment = 17;

    private static IPMatcher ip6FullMatcher;
    private static IPMatcher ip6MaskedMatcher;

    /**
     * This also tests instantiation of correct masked and unmasked IPv6 addresses.
     *
     * @throws IPMatcherException if there is an error parsing the specification (i.e. it is
     *                            somehow malformed)
     */
    @BeforeClass
    static public void setUp() throws IPMatcherException {
        ip6FullMatcher = new IPMatcher(IP6_FULL_ADDRESS1);
        ip6MaskedMatcher = new IPMatcher(IP6_MASKED_ADDRESS);
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected = IPMatcherException.class)
    public void testIPMatcherIp6Incomplete()
        throws IPMatcherException {
        new IPMatcher("1234:5"); // Incomplete IPv6 address
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected = IPMatcherException.class)
    public void testIPMatcherIp6MaskOutOfRange()
        throws IPMatcherException {
        new IPMatcher("123::456/999"); // Mask bits out of range
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected = IPMatcherException.class)
    public void testIPMatcherIp6MaskNotNumeric()
        throws IPMatcherException {
        new IPMatcher("123::456/abc"); // Mask is not a number
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected = IPMatcherException.class)
    public void testIPMatcherIp6TooManySlashes()
        throws IPMatcherException {
        new IPMatcher("123::456/12/12"); // Too many slashes
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6FullMatch()
        throws IPMatcherException {
        assertTrue("IPv6 full match fails", ip6FullMatcher
            .match(IP6_FULL_ADDRESS1));
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6MisMatch()
        throws IPMatcherException {
        assertFalse("IPv6 full nonmatch succeeds", ip6FullMatcher
            .match(IP6_FULL_ADDRESS2));
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6MaskedMatch()
        throws IPMatcherException {
        assertTrue("IPv6 masked match fails", ip6MaskedMatcher
            .match(IP6_FULL_ADDRESS2));
    }

    @Test
    public void testIPv4MatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("1.1.1.1");

        assertTrue(ipMatcher.match("1.1.1.1"));
        ArrayList<String> exceptions = new ArrayList<String>();
        exceptions.add("1.1.1.1");
        verifyAllIp4Except(exceptions, false, ipMatcher);
    }

    @Test
    public void testIPv4MatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("1.1.1.1");

        assertFalse(ipMatcher.match("1.1.1.0"));
    }

    @Test
    public void testIPv6MatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("::2");

        assertTrue(ipMatcher.match("0:0:0:0:0:0:0:2"));
    }

    @Test
    public void testShortFormIPv6MatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("::2");

        assertTrue(ipMatcher.match("::2"));
    }

    @Test
    public void testIPv6MatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("::2");

        assertFalse(ipMatcher.match("0:0:0:0:0:0:0:1"));
    }

    @Test
    public void testIPv6FullMaskMatching() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("::2/128");

        assertTrue(ipMatcher.match("0:0:0:0:0:0:0:2"));
        assertFalse(ipMatcher.match("0:0:0:0:0:0:0:1"));
    }


    @Test
    public void testAsteriskMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("172.16");

        assertTrue(ipMatcher.match("172.16.1.1"));
    }

    @Test
    public void testAsteriskMatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("172.16");

        assertFalse(ipMatcher.match("172.15.255.255"));
    }

    @Test
    public void testIPv4CIDRMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.1.2.3/8");

        assertTrue(ipMatcher.match("192.1.1.1"));
    }

    @Test
    public void testIPv4CIDRMatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.1.2.3/8");

        assertTrue(ipMatcher.match("192.2.0.0"));
    }

    @Test
    public void test2IPv4CIDRMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.86.100.72/29");

        assertTrue(ipMatcher.match("192.86.100.75"));
        assertFalse(ipMatcher.match("192.86.100.71"));
        assertFalse(ipMatcher.match("192.86.100.80"));
        ArrayList<String> exceptions = new ArrayList<String>();
        exceptions.add("192.86.100.72");
        exceptions.add("192.86.100.73");
        exceptions.add("192.86.100.74");
        exceptions.add("192.86.100.75");
        exceptions.add("192.86.100.76");
        exceptions.add("192.86.100.77");
        exceptions.add("192.86.100.78");
        exceptions.add("192.86.100.79");
        verifyAllIp4Except(exceptions, false, ipMatcher);
    }

    @Test
    public void test3IPv4CIDRMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.86.100.72/255.255.255.248");

        assertTrue(ipMatcher.match("192.86.100.75"));
        assertFalse(ipMatcher.match("192.86.100.71"));
        assertFalse(ipMatcher.match("192.86.100.80"));
        ArrayList<String> exceptions = new ArrayList<String>();
        exceptions.add("192.86.100.72");
        exceptions.add("192.86.100.73");
        exceptions.add("192.86.100.74");
        exceptions.add("192.86.100.75");
        exceptions.add("192.86.100.76");
        exceptions.add("192.86.100.77");
        exceptions.add("192.86.100.78");
        exceptions.add("192.86.100.79");
        verifyAllIp4Except(exceptions, false, ipMatcher);
    }

    @Test
    public void testIPv6CIDRMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("0:0:0:1::/64");

        assertTrue(ipMatcher.match("0:0:0:1:ffff:ffff:ffff:ffff"));
    }

    @Test
    public void testIPv6CIDRMatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("0:0:0:1::/64");

        assertFalse(ipMatcher.match("0:0:0:2::"));
    }


    @Test
    public void testIPv4IPv6Matching() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("0.0.0.1");

        assertTrue(ipMatcher.match("::1"));
    }


    @Test
    public void testSubnetZeroIPv6CIDRMatching() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("::1/0");

        assertTrue(ipMatcher.match("::2"));
    }

    @Test
    public void testAllOnesSubnetIPv4CIDRMatchingSuccess() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.1.2.3/32");

        assertTrue(ipMatcher.match("192.1.2.3"));
    }

    @Test
    public void testAllOnesSubnetIPv4CIDRMatchingFailure() throws Exception {
        final IPMatcher ipMatcher = new IPMatcher("192.1.2.3/32");

        assertFalse(ipMatcher.match("192.1.2.2"));
    }

    // Commented out as this is currently not used in tests
    /*private ArrayList<String> getAllIp4Except(ArrayList<String> exceptions) {
        int d1 = 0;
        int d2 = 0;
        int d3 = 0;
        int d4 = 0;
        ArrayList<String> ips = new ArrayList<String>();
        for (d1 = 0; d1 <= 255; d1 += increment) {
            for (d2 = 0; d2 <= 255; d2 += increment) {
                for (d3 = 0; d3 <= 255; d3 += increment) {
                    for (d4 = 0; d4 <= 255; d4 += increment) {
                        String IP = d1 + "." + d2 + "." + d3 + "." + d4;
                        if (exceptions == null || !exceptions.contains(IP)) {
                            ips.add(IP);
                        }
                    }
                }
            }
        }
        return ips;
    }*/

    private void verifyAllIp4Except(ArrayList<String> exceptions, boolean asserted, IPMatcher ipMatcher)
        throws IPMatcherException {
        int d1 = 0;
        int d2 = 0;
        int d3 = 0;
        int d4 = 0;
        for (d1 = 0; d1 <= 255; d1 += increment) {
            for (d2 = 0; d2 <= 255; d2 += increment) {
                for (d3 = 0; d3 <= 255; d3 += increment) {
                    for (d4 = 0; d4 <= 255; d4 += increment) {
                        String IP = d1 + "." + d2 + "." + d3 + "." + d4;
                        if (exceptions != null && exceptions.contains(IP)) {
                            if (asserted) {
                                assertFalse(ipMatcher.match(IP));
                            } else {
                                assertTrue(ipMatcher.match(IP));
                            }
                        } else {
                            if (asserted) {
                                assertTrue(ipMatcher.match(IP));
                            } else {
                                assertFalse(ipMatcher.match(IP));
                            }
                        }

                    }
                }
            }
        }
    }


    @AfterClass
    static public void cleanup() {
        ip6FullMatcher = null;
        ip6MaskedMatcher = null;
    }
}
