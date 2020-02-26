/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Perform some basic unit tests for Utils Class
 *
 * @author tdonohue
 */
public class UtilsTest extends AbstractUnitTest {

    /**
     * Test of getBaseUrl method, of class Utils
     */
    @Test
    public void testGetBaseUrl() {
        assertEquals("Test remove /server", "http://dspace.org",
                     Utils.getBaseUrl("http://dspace.org/server"));

        assertEquals("Test remove /server/api/core/items", "https://dspace.org",
                     Utils.getBaseUrl("https://dspace.org/server/api/core/items"));

        assertEquals("Test remove trailing slash", "https://dspace.org",
                     Utils.getBaseUrl("https://dspace.org/"));

        assertEquals("Test keep url", "https://demo.dspace.org",
                     Utils.getBaseUrl("https://demo.dspace.org"));

        // This uses a bunch of reserved URI characters
        assertNull("Test invalid URI returns null", Utils.getBaseUrl("&+,?/@="));
    }

    /**
     * Test of getHostName method, of class Utils
     */
    @Test
    public void testGetHostName() {
        assertEquals("Test remove HTTP", "dspace.org",
                     Utils.getHostName("http://dspace.org"));

        assertEquals("Test remove HTTPS", "dspace.org",
                     Utils.getHostName("https://dspace.org"));

        assertEquals("Test remove trailing slash", "dspace.org",
                     Utils.getHostName("https://dspace.org/"));

        assertEquals("Test remove www.", "dspace.org",
                     Utils.getHostName("https://www.dspace.org"));

        assertEquals("Test keep other prefixes", "demo.dspace.org",
                     Utils.getHostName("https://demo.dspace.org"));

        // This uses a bunch of reserved URI characters
        assertNull("Test invalid URI returns null", Utils.getHostName("&+,?/@="));
    }

    /**
     * Test of interpolateConfigsInString method, of class Utils
     */
    @Test
    public void testInterpolateConfigsInString() {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Add a new config to test with
        String configName = "not.a.dspace.config.at.all";
        String configValue = "demo.dspace.org";
        configurationService.setProperty(configName, configValue);

        // Create a string where the config is represented by ${variable}
        String stringWithVariable = "The config " + configName + " has a value of ${" + configName + "}!";
        String expectedValue = "The config " + configName + " has a value of " + configValue + "!";

        assertEquals("Test config interpolation", expectedValue,
                     Utils.interpolateConfigsInString(stringWithVariable));

        // remove the config we added
        configurationService.setProperty(configName, null);
    }
}
