/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author mwood
 */
public class SpiderDetectorTest extends AbstractDSpaceTest {
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    @BeforeEach
    public void init() {
        // Get current configuration
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Ensure useProxies is set to false for all tests
        configurationService.setProperty("useProxies", false);
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#readPatterns(java.io.File)}.
     */
    @Test
    public void testReadPatterns() {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#getSpiderIpAddresses()}.
     */
    @Test
    public void testGetSpiderIpAddresses() {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetector#isSpider(jakarta.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testIsSpiderHttpServletRequest() {
        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is watching you");
        assertTrue(SpiderDetector.isSpider(req), "'msnbot' did not match any pattern");

        req.setAgent("Firefox");
        assertFalse(SpiderDetector.isSpider(req), "'Firefox' matched a pattern");

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(SpiderDetector.isSpider(req), candidate + " did not match IP patterns");

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(SpiderDetector.isSpider(req), NOT_A_BOT_ADDRESS + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(SpiderDetector.isSpider(req), candidate + " did not match DNS patterns");

        candidate = "wiki.dspace.org";
        req.setRemoteHost(candidate);
        assertFalse(SpiderDetector.isSpider(req), candidate + " matched DNS patterns");
    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetector#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString() {
        String candidate;

        // Test IP patterns
        candidate = "192.168.2.1";
        assertTrue(SpiderDetector.isSpider(candidate, null, null, null),
                   candidate + " did not match IP patterns");

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(SpiderDetector.isSpider(candidate, null, null, null),
                    candidate + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null),
                   candidate + " did not match DNS patterns");

        candidate = "wiki.dspace.org";
        assertFalse(SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null),
                    candidate + " matched DNS patterns");

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue(SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate),
                   "'" + candidate + "' did not match agent patterns");

        candidate = "Firefox";
        assertFalse(SpiderDetector.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate),
                    "'" + candidate + "' matched agent patterns");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#isSpider(java.lang.String)}.
     */
    @Test
    public void testIsSpiderString() {
        String candidate;

        candidate = "192.168.2.1";
        assertTrue(SpiderDetector.isSpider(candidate, null, null, null),
                   candidate + " did not match IP patterns");

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(SpiderDetector.isSpider(candidate, null, null, null),
                    candidate + " matched IP patterns");

    }
}
