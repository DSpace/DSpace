/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.ArrayList;
import java.util.List;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mwood
 */
public class SpiderDetectorTest
{

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
        Mockit.setUpMocks(MockSolrLogger.class); // Don't test SolrLogger here

        final String NOT_A_BOT_ADDRESS = "192.168.0.1";

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
