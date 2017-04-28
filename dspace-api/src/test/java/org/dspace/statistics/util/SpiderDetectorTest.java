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
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mwood
 */
@RunWith(MockitoJUnitRunner.class)
public class SpiderDetectorTest extends AbstractDSpaceTest
{
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    @org.mockito.Mock
    private ConfigurationService configurationService;
    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetector#readPatterns(java.io.File)}.
     */

    private SpiderDetector spiderDetector;

    @Before
    public void init() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        spiderDetector = new SpiderDetector(configurationService);

    }

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

    @Test
    public void testCaseInsensitiveMatching() throws Exception
    {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetector = new SpiderDetector(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("FirefOx");
        assertFalse("'Firefox' matched a pattern", spiderDetector.isSpiderAgent(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetector.isSpiderAgent(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetector.isSpiderAgent(req));

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
        assertTrue("'msnbot' did not match any pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("Firefox");
        assertFalse("'Firefox' matched a pattern", spiderDetector.isSpiderAgent(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetector.isSpiderAgent(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "wiki.dspace.org";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetector.isSpiderAgent(req));
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
                spiderDetector.isSpiderAgent(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetector.isSpiderAgent(candidate, null, null, null));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(candidate + " did not match DNS patterns",
                spiderDetector.isSpiderAgent(NOT_A_BOT_ADDRESS, null, candidate, null));

        candidate = "wiki.dspace.org";
        assertFalse(candidate + " matched DNS patterns",
                spiderDetector.isSpiderAgent(NOT_A_BOT_ADDRESS, null, candidate, null));

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue("'" + candidate + "' did not match agent patterns",
                spiderDetector.isSpiderAgent(NOT_A_BOT_ADDRESS, null, null, candidate));

        candidate = "Firefox";
        assertFalse("'" + candidate + "' matched agent patterns",
                spiderDetector.isSpiderAgent(NOT_A_BOT_ADDRESS, null, null, candidate));
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
                spiderDetector.isSpiderAgent(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetector.isSpiderAgent(candidate, null, null, null));

    }



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
        assertFalse("'msnbot' matched pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("FirefOx");
        assertFalse("'Firefox' matched a pattern", spiderDetector.isSpiderAgent(req));

        // Test IP patterns
        candidate = "192.168.2.1";
        req.setAddress(candidate);
        assertTrue(candidate + " did not match IP patterns", spiderDetector.isSpiderAgent(req));

        req.setAddress(NOT_A_BOT_ADDRESS);
        assertFalse(NOT_A_BOT_ADDRESS + " matched IP patterns", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " did match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "wIki.dsPace.oRg";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetector.isSpiderAgent(req));

    }

    /**
     * Test to see that lowercased will be matched but uppercase won't
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
        assertTrue("'msnbot' didn't match pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse("'msnbot' matched pattern", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetector.isSpiderAgent(req));
    }

    /**
     * Test to check if the same agent gets caught with and without upper-casing
     */
    @Test
    public void testBothLowerAndUpperCaseGetMatched() {

        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetector = new SpiderDetector(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " didn't match DNS patterns", spiderDetector.isSpiderAgent(req));
    }

    /**
     * Test if wrong value is used for property
     */
    @Test
    public void testNonBooleanConfig() {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", "RandomNonBooleanString");
        spiderDetector = new SpiderDetector(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnbot is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetector.isSpiderAgent(req));

        req.setAgent("MSNBOT Is WaTching you");
        assertFalse("'msnbot' matched pattern", spiderDetector.isSpiderAgent(req));

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        req.setRemoteHost(candidate);
        assertTrue(candidate + " did not match DNS patterns", spiderDetector.isSpiderAgent(req));

        candidate = "baiduspiDer-dSPace-test.crawl.baIDu.com";
        req.setRemoteHost(candidate);
        assertFalse(candidate + " matched DNS patterns", spiderDetector.isSpiderAgent(req));


    }



    /**
     * Method to make sure the SpiderDetector is using CaseSensitive matching again after each test
     * @throws Exception
     */
    @After
    public void cleanup() throws Exception {
        spiderDetector = null;
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
