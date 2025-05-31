/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.handle.hdllib.HandleValue;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class HandlePluginTest
        extends AbstractUnitTest {
    private static HandlePlugin plugin;

    ////////////////////////////////////////
    // Test suite initialization and termination
    ////////////////////////////////////////

    /**
     * Initialize for all tests.
     *
     * @throws Exception passed through.
     */
    @BeforeClass
    public static void setupClass()
            throws Exception {
        plugin = new HandlePlugin();
        plugin.init(null);
    }

    @AfterClass
    public static void shutdownClass() {
        plugin.shutdown();
    }

    ////////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    ////////////////////////////////////////

    /**
     * Test of init method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testInit() throws Exception {
    }

    /**
     * Test of setHaveNA method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testSetHaveNA() throws Exception {
    }

    /**
     * Test of createHandle method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testCreateHandle() throws Exception {
    }

    /**
     * Test of deleteHandle method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testDeleteHandle() throws Exception {
    }

    /**
     * Test of updateValue method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testUpdateValue() throws Exception {
    }

    /**
     * Test of deleteAllRecords method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testDeleteAllRecords() throws Exception {
    }

    /**
     * Test of checkpointDatabase method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testCheckpointDatabase() throws Exception {
    }

    /**
     * Test of shutdown method, of class HandlePlugin.
     */
    @Ignore
    @Test
    public void testShutdown() {
    }

    /**
     * Test of scanHandles method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testScanHandles() throws Exception {
    }

    /**
     * Test of scanNAs method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testScanNAs() throws Exception {
    }

    ////////////////////////////////////////
    // Resolving methods -- these are implemented.
    ////////////////////////////////////////

    /**
     * Test of getRawHandleValues method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetRawHandleValues() throws Exception {
        // Get a Handle from something.
        final Site site = ContentServiceFactory.getInstance()
                .getSiteService()
                .findSite(context);
        final String siteHandle = site.getHandle();
        final String siteUUID = site.getID().toString();

        // Look up the Handle.
        byte[][] handles = plugin.getRawHandleValues(siteHandle.getBytes(), null, null);

        // Test that the URL value is as expected.
        byte[] returned = handles[0];
        HandleValue handleValue = new HandleValue();
        net.handle.hdllib.Encoder.decodeHandleValue(returned, 0, handleValue);
        String handleText = handleValue.getDataAsString();
        URIBuilder uriBuilder = new URIBuilder(handleText);
        List<String> pathSegments = uriBuilder.getPathSegments();
        assertEquals("Site URL wrong number of path segments",
                pathSegments.size(), 2);
        assertEquals("Site URL does not point to /sites",
                pathSegments.get(0), "sites");
        assertEquals("Site URL wrong UUID",
                pathSegments.get(1), siteUUID);

        // TODO test other parts of the HandleValue
    }

    /**
     * Test of haveNA method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testHaveNA() throws Exception {
    }

    /**
     * Test of getHandlesForNA method, of class HandlePlugin.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testGetHandlesForNA() throws Exception {
    }

}
