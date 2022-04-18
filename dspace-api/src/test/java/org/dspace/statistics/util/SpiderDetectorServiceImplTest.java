/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

<<<<<<< HEAD

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
=======
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
>>>>>>> dspace-7.2.1

/**
 * @author mwood
 * @author frederic at atmire.com
 */
<<<<<<< HEAD
@RunWith(MockitoJUnitRunner.class)
public class SpiderDetectorServiceImplTest extends AbstractDSpaceTest
{
=======
public class SpiderDetectorServiceImplTest extends AbstractDSpaceTest {
>>>>>>> dspace-7.2.1
    private static final String NOT_A_BOT_ADDRESS = "192.168.0.1";

    private ConfigurationService configurationService;

<<<<<<< HEAD
=======
    private ClientInfoService clientInfoService;
>>>>>>> dspace-7.2.1

    private SpiderDetectorService spiderDetectorService;

    @Before
    public void init() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
<<<<<<< HEAD
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);

    }

    @Test
    public void testReadPatterns()
    {
=======
        clientInfoService = CoreServiceFactory.getInstance().getClientInfoService();
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);
    }

    @Test
    public void testReadPatterns() {
>>>>>>> dspace-7.2.1
// FIXME        fail("Not yet implemented");
    }

    @Test
<<<<<<< HEAD
    public void testGetSpiderIpAddresses()
    {
=======
    public void testGetSpiderIpAddresses() {
>>>>>>> dspace-7.2.1
// FIXME        fail("Not yet implemented");
    }

    /**
     * Test if Case Insitive matching option works
<<<<<<< HEAD
     * @throws Exception
     */
    @Test
    public void testCaseInsensitiveMatching() throws Exception
    {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);
=======
     *
     * @throws Exception
     */
    @Test
    public void testCaseInsensitiveMatching() throws Exception {
        configurationService.setProperty("usage-statistics.bots.case-insensitive", true);
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);
>>>>>>> dspace-7.2.1

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress(NOT_A_BOT_ADDRESS); // avoid surprises
        req.setRemoteHost("notabot.example.com"); // avoid surprises
        req.setAgent("Firefox"); // avoid surprises

        String candidate;

        // Test agent patterns
        req.setAgent("msnboT Is WaTching you");
        assertTrue("'msnbot' didn't match pattern", spiderDetectorService.isSpider(req));

<<<<<<< HEAD
        req.setAgent("FirefOx");
=======
        req.setAgent("mozilla/5.0 (x11; linux x86_64; rv:91.0) gecko/20100101 firefox/91.0");
>>>>>>> dspace-7.2.1
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
<<<<<<< HEAD
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testIsSpiderHttpServletRequest()
    {
=======
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testIsSpiderHttpServletRequest() {
>>>>>>> dspace-7.2.1
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
<<<<<<< HEAD
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString()
    {
=======
     * Test method for
     * {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSpiderStringStringStringString() {
>>>>>>> dspace-7.2.1
        String candidate;

        // Test IP patterns
        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
<<<<<<< HEAD
                spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));
=======
                   spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                    spiderDetectorService.isSpider(candidate, null, null, null));
>>>>>>> dspace-7.2.1

        // Test DNS patterns
        candidate = "baiduspider-dspace-test.crawl.baidu.com";
        assertTrue(candidate + " did not match DNS patterns",
<<<<<<< HEAD
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        candidate = "wiki.dspace.org";
        assertFalse(candidate + " matched DNS patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));
=======
                   spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));

        candidate = "wiki.dspace.org";
        assertFalse(candidate + " matched DNS patterns",
                    spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, candidate, null));
>>>>>>> dspace-7.2.1

        // Test agent patterns
        candidate = "msnbot is watching you";
        assertTrue("'" + candidate + "' did not match agent patterns",
<<<<<<< HEAD
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));

        candidate = "Firefox";
        assertFalse("'" + candidate + "' matched agent patterns",
                spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));
=======
                   spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));

        candidate = "Firefox";
        assertFalse("'" + candidate + "' matched agent patterns",
                    spiderDetectorService.isSpider(NOT_A_BOT_ADDRESS, null, null, candidate));
>>>>>>> dspace-7.2.1
    }

    /**
     * Test method for {@link org.dspace.statistics.util.SpiderDetectorService#isSpider(java.lang.String)}.
     */
    @Test
<<<<<<< HEAD
    public void testIsSpiderString()
    {
=======
    public void testIsSpiderString() {
>>>>>>> dspace-7.2.1
        String candidate;

        candidate = "192.168.2.1";
        assertTrue(candidate + " did not match IP patterns",
<<<<<<< HEAD
                spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                spiderDetectorService.isSpider(candidate, null, null, null));
=======
                   spiderDetectorService.isSpider(candidate, null, null, null));

        candidate = NOT_A_BOT_ADDRESS;
        assertFalse(candidate + " matched IP patterns",
                    spiderDetectorService.isSpider(candidate, null, null, null));
>>>>>>> dspace-7.2.1

    }


    /**
     * Test if Case Sensitive matching still works after adding the option
<<<<<<< HEAD
     * @throws Exception
     */
    @Test
    public void testCaseSensitiveMatching() throws Exception
    {
=======
     *
     * @throws Exception
     */
    @Test
    public void testCaseSensitiveMatching() throws Exception {
>>>>>>> dspace-7.2.1

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
<<<<<<< HEAD
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);
=======
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);
>>>>>>> dspace-7.2.1

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
<<<<<<< HEAD
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService);
=======
        spiderDetectorService = new SpiderDetectorServiceImpl(configurationService, clientInfoService);
>>>>>>> dspace-7.2.1

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


<<<<<<< HEAD

    /**
     * Method to make sure the SpiderDetector is using CaseSensitive matching again after each test
=======
    /**
     * Method to make sure the SpiderDetector is using CaseSensitive matching again after each test
     *
>>>>>>> dspace-7.2.1
     * @throws Exception
     */
    @After
    public void cleanup() throws Exception {
        spiderDetectorService = null;
<<<<<<< HEAD
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

=======
        configurationService.setProperty("usage-statistics.bots.case-insensitive", false);
    }
>>>>>>> dspace-7.2.1
}
