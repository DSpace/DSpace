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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;

/**
 * @author mwood
 * @author frederic at atmire.com
 */
@RunWith(MockitoJUnitRunner.class)
public class SpiderDetectorServiceImplTest extends AbstractDSpaceTest
{
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    private ConfigurationService configurationService;


    private SpiderDetectorService spiderDetectorService;

    @Before
    public void init() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);

    }

    @Test
    public void testReadPatterns()
    {
// FIXME        fail("Not yet implemented");
    }

    @Test
    public void testGetSpiderIpAddresses()
    {
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test if Case Insitive matching option works
     * @throws Exception
     */
    @Test
    public void testCaseInsensitiveMatching() throws Exception
    {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

        req.setAgent("FirefOx");
        assertFalse("'Firefox' matched a pattern", spiderDetectorService.isSpider(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetectorService.isSpider(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetectorService.isSpider(req));

    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(javax.servlet.http.HttpServletRequest)}.
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
        assertTrue("'msnbot' did not match any pattern", spiderDetectorService.isSpider(req));

        req.setAgent("Firefox");
        assertFalse("'Firefox' matched a pattern", spiderDetectorService.isSpider(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetectorService.isSpider(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "wiki.dspace.org";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetectorService.isSpider(req));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString()
    {
        String candidate;

        // Test IP patterns
        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(candidate + " did not match DNS patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        candidate = "wiki.dspace.org";
        assertFalse(candidate + " matched DNS patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue("'" + candidate + "' did not match agent patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));

        candidate = "Firefox";
        assertFalse("'" + candidate + "' matched agent patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String)}.
     */
    @Test
    public void testIsSpiderString()
    {
        String candidate;

        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));

    }


    /**
     * Test if Case Sensitive matching still works after adding the option
     * @throws Exception
     */
    @Test
    public void testCaseSensitiveMatching() throws Exception
    {

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertFalse("'msnbot' matched pattern", spiderDetectorService.isSpider(req));

        req.setAgent("FirefOx");
        assertFalse("'Firefox' matched a pattern", spiderDetectorService.isSpider(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetectorService.isSpider(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " did match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetectorService.isSpider(req));

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
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse("'msnbot' matched pattern", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetectorService.isSpider(req));
    }

    /**
     * Test to check if the same agent gets caught with and without upper-casing
     */
    @Test
    public void testBothLowerAndUpperCaseGetMatched() {

        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " didn't match DNS patterns", spiderDetectorService.isSpider(req));
    }

    /**
     * Test if wrong value is used for property
     */
    @Test
    public void testNonBooleanConfig() {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", "RandomNonBooleanString");
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse("'msnbot' matched pattern", spiderDetectorService.isSpider(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetectorService.isSpider(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetectorService.isSpider(req));


    }



    /**
     * Method to make sure the SpiderDetector is using CaseSensitive matching again after each test
     * @throws Exception
     */
    @After
    public void cleanup() throws Exception {
        spiderDetectorService = null;
        configurationService.setProperty("usage-statistics.bots.case-insensitive", false);;
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