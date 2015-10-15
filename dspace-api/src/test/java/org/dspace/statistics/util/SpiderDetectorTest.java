/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import mockit.Mock;
import mockit.MockUp;
import org.dspace.AbstractDSpaceTest;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mwood
 */
public class SpiderDetectorTest extends AbstractDSpaceTest
{
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#readPatterns(java.io.File)}.
     */
    @Test
    public void testReadPatterns()
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
        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is watching you");
        assertTrue("'msnbot' did not match any pattern", SpiderDetector.isSpider(req));

        req.setAgent("Firefox");
        assertFalse("'Firefox' matched a pattern", SpiderDetector.isSpider(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", SpiderDetector.isSpider(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", SpiderDetector.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", SpiderDetector.isSpider(req));

        candidate = "wiki.dspace.org";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", SpiderDetector.isSpider(req));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString()
    {
        String candidate;

        // Test IP patterns
        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
                SpiderDetector.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                SpiderDetector.isSpider(candidate, null, null, null));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(candidate + " did not match DNS patterns",
                SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        candidate = "wiki.dspace.org";
        assertFalse(candidate + " matched DNS patterns",
                SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue("'" + candidate + "' did not match agent patterns",
                SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));

        candidate = "Firefox";
        assertFalse("'" + candidate + "' matched agent patterns",
                SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#isSpider(java.lang.String)}.
     */
    @Test
    public void testIsSpiderString()
    {
        String candidate;

        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
                SpiderDetector.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                SpiderDetector.isSpider(candidate, null, null, null));

    }

    /**
     * Dummy SolrLogger for testing.
     * @author mwood
     */
    static public class MockSolrLogger
            extends MockUp<SolrLoggerServiceImpl>
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
