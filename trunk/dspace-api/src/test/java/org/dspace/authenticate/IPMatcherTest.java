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

import org.dspace.AbstractUnitTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Mark Wood
 */
public class IPMatcherTest extends AbstractUnitTest
{
    private static final String IP6_FULL_ADDRESS1 = "2001:18e8:3:171:218:8bff:fe2a:56a4";
    private static final String IP6_FULL_ADDRESS2 = "2001:18e8:3:171:218:8bff:fe2a:56a3";
    private static final String IP6_MASKED_ADDRESS = "2001:18e8:3::/48";

    private static IPMatcher ip6FullMatcher;
    private static IPMatcher ip6MaskedMatcher;

    /**
     * This also tests instantiation of correct masked and unmasked IPv6 addresses.
     * @throws IPMatcherException
     */
    @BeforeClass
    static public void setUp() throws IPMatcherException
    {
        ip6FullMatcher = new IPMatcher(IP6_FULL_ADDRESS1);
        ip6MaskedMatcher = new IPMatcher(IP6_MASKED_ADDRESS);
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected=IPMatcherException.class)
    public void testIPMatcherIp6Incomplete()
    throws IPMatcherException
    {
        new IPMatcher("1234:5"); // Incomplete IPv6 address
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected=IPMatcherException.class)
    public void testIPMatcherIp6MaskOutOfRange()
    throws IPMatcherException
    {
        new IPMatcher("123::456/999"); // Mask bits out of range
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected=IPMatcherException.class)
    public void testIPMatcherIp6MaskNotNumeric()
    throws IPMatcherException
    {
        new IPMatcher("123::456/abc"); // Mask is not a number
    }

    /**
     * Test method for {@link org.dspace.authenticate.IPMatcher#IPMatcher(java.lang.String)}.
     */
    @Test(expected=IPMatcherException.class)
    public void testIPMatcherIp6TooManySlashes()
    throws IPMatcherException
    {
        new IPMatcher("123::456/12/12"); // Too many slashes
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6FullMatch()
    throws IPMatcherException
    {
        assertTrue("IPv6 full match fails", ip6FullMatcher
                .match(IP6_FULL_ADDRESS1));
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6MisMatch()
    throws IPMatcherException
    {
        assertFalse("IPv6 full nonmatch succeeds", ip6FullMatcher
                .match(IP6_FULL_ADDRESS2));
    }

    /**
     * Test method for
     * {@link org.dspace.authenticate.IPMatcher#match(java.lang.String)}.
     */
    @Test
    public void testIp6MaskedMatch()
    throws IPMatcherException
    {
        assertTrue("IPv6 masked match fails", ip6MaskedMatcher
                .match(IP6_FULL_ADDRESS2));
    }
    
    @AfterClass
    static public void cleanup()
    {
        ip6FullMatcher = null;
        ip6MaskedMatcher = null;
    }
}
