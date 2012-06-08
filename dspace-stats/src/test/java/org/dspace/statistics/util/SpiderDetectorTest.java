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

import java.util.ArrayList;
import java.util.List;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.junit.Test;

/**
 * @author mwood
 */
public class SpiderDetectorTest
{

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#readIpAddresses(java.io.File)}.
     */
    @Test
    public void testReadIpAddresses()
    {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#setAgentPatterns(java.util.List)}.
     */
    @Test
    public void testSetAgentPatterns()
    {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#getSpiderIpAddresses()}.
     */
    @Test
    public void testGetSpiderIpAddresses()
    {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#isSpider(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testIsSpiderHttpServletRequest()
    {
        Mockit.setUpMocks(MockSolrLogger.class); // Don't test SolrLogger here

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress("192.168.0.1"); // avoid surprises

        List<String> testPatterns = new ArrayList<String>();
        testPatterns.add("^msnbot");
        SpiderDetector.clearAgentPatterns(); // start fresh, in case Spring is active
        SpiderDetector.setAgentPatterns(testPatterns);

        req.setAgent("msnbot is watching you");
        assertTrue("'msnbot' did not match any pattern", SpiderDetector.isSpider(req));

        req.setAgent("Firefox");
        assertFalse("'Firefox' matched a pattern", SpiderDetector.isSpider(req));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#isSpider(java.lang.String)}.
     */
    @Test
    public void testIsSpiderString()
    {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Dummy SolrLogger for testing.
     * @author mwood
     */
    @MockClass (realClass = org.dspace.statistics.SolrLogger.class)
    static public class MockSolrLogger
    {
        @Mock
        public void $init() {}

        @Mock
        public void $clinit() {}
        
        @Mock
        public boolean isUseProxies()
        {
            return false;
        }
    }
}
