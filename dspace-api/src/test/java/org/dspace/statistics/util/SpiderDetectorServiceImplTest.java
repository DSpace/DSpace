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
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author mwood
 * @author frederic at atmire.com
 */
public class SpiderDetectorServiceImplTest extends AbstractDSpaceTest {
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    private ConfigurationService configurationService;

    private ClientInfoService clientInfoService;

    private SpiderDetectorService spiderDetectorService;

    @BeforeEach
    public void init() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        clientInfoService = CoreServiceFactory.getInstance().getClientInfoService();
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);
    }

    @Test
    public void testReadPatterns() {
// FIXME        fail("Not yet implemented");
    }

    @Test
    public void testGetSpiderIpAddresses() {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test if Case Insitive matching option works
     *
     * @throws Exception
     */
    @Test
    public void testCaseInsensitiveMatching() throws Exception {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' didn't match pattern");

        req.setAgent("mozilla/5.0 (x11; linux x86_64; rv:91.0) gecko/20100101 firefox/91.0");
        assertFalse(spiderDetectorService.isSpider(req), "'Firefox' matched a pattern");

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match IP patterns");

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(spiderDetectorService.isSpider(req), NOT_A_BOT_ADDRESS + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did match DNS patterns");

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " matched DNS patterns");

    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(jakarta.servlet.http.HttpServletRequest)}.
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
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' did not match any pattern");

        req.setAgent("Firefox");
        assertFalse(spiderDetectorService.isSpider(req), "'Firefox' matched a pattern");

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match IP patterns");

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(spiderDetectorService.isSpider(req), NOT_A_BOT_ADDRESS + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match DNS patterns");

        candidate = "wiki.dspace.org";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " matched DNS patterns");
    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString() {
        String candidate;

        // Test IP patterns
        candidate = "192.168.2.1";
        assertTrue(spiderDetectorService.isSpider(candidate, null, null, null),
                   candidate + " did not match IP patterns");

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(spiderDetectorService.isSpider(candidate, null, null, null),
                    candidate + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null),
                   candidate + " did not match DNS patterns");

        candidate = "wiki.dspace.org";
        assertFalse(spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null),
                    candidate + " matched DNS patterns");

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue(spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate),
                   "'" + candidate + "' did not match agent patterns");

        candidate = "Firefox";
        assertFalse(spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate),
                    "'" + candidate + "' matched agent patterns");
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String)}.
     */
    @Test
    public void testIsSpiderString() {
        String candidate;

        candidate = "192.168.2.1";
        assertTrue(spiderDetectorService.isSpider(candidate, null, null, null),
                   candidate + " did not match IP patterns");

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(spiderDetectorService.isSpider(candidate, null, null, null),
                    candidate + " matched IP patterns");

    }


    /**
     * Test if Case Sensitive matching still works after adding the option
     *
     * @throws Exception
     */
    @Test
    public void testCaseSensitiveMatching() throws Exception {

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertFalse(spiderDetectorService.isSpider(req), "'msnbot' matched pattern");

        req.setAgent("FirefOx");
        assertFalse(spiderDetectorService.isSpider(req), "'Firefox' matched a pattern");

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match IP patterns");

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(spiderDetectorService.isSpider(req), NOT_A_BOT_ADDRESS + " matched IP patterns");

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " did match DNS patterns");

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " matched DNS patterns");

    }

    /**
     * Test to see that lowercase will be matched but uppercase won't
     */
    @Test
    public void testInsensitiveSensitiveDifference() {

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' didn't match pattern");

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse(spiderDetectorService.isSpider(req), "'msnbot' matched pattern");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match DNS patterns");

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " matched DNS patterns");
    }

    /**
     * Test to check if the same agent gets caught with and without upper-casing
     */
    @Test
    public void testBothLowerAndUpperCaseGetMatched() {

        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' didn't match pattern");

        req.setAgent("MSNBOT Is WaTching you");
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' didn't match pattern");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match DNS patterns");

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " didn't match DNS patterns");
    }

    /**
     * Test if wrong value is used for property
     */
    @Test
    public void testNonBooleanConfig() {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", "RandomNonBooleanString");
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue(spiderDetectorService.isSpider(req), "'msnbot' didn't match pattern");

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse(spiderDetectorService.isSpider(req), "'msnbot' matched pattern");

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(spiderDetectorService.isSpider(req), candidate + " did not match DNS patterns");

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(spiderDetectorService.isSpider(req), candidate + " matched DNS patterns");


    }


    /**
     * Method to make sure the SpiderDetector is using CaseSensitive matching again after each test
     *
     * @throws Exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        spiderDetectorService = null;
        configurationService.setProperty("usage-statistics.bots.case-insensitive", false);
    }
}
