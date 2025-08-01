/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
        assertEquals("http://dspace.org",
                     Utils.getBaseUrl("http://dspace.org/server"),
                     "Test remove /server");

        assertEquals("https://dspace.org",
                     Utils.getBaseUrl("https://dspace.org/server/api/core/items"),
                     "Test remove /server/api/core/items");

        assertEquals("https://dspace.org",
                     Utils.getBaseUrl("https://dspace.org/"),
                     "Test remove trailing slash");

        assertEquals("https://demo.dspace.org",
                     Utils.getBaseUrl("https://demo.dspace.org"),
                     "Test keep url");

        assertEquals("http://localhost:8080",
                     Utils.getBaseUrl("http://localhost:8080"),
                     "Test keep url");

        assertEquals("http://localhost:8080",
                     Utils.getBaseUrl("http://localhost:8080/server"),
                     "Test keep url");

        // This uses a bunch of reserved URI characters
        assertNull(Utils.getBaseUrl("&+,?/@="), "Test invalid URI returns null");
    }

    /**
     * Test of getHostName method, of class Utils
     */
    @Test
    public void testGetHostName() {
        assertEquals("dspace.org",
                     Utils.getHostName("http://dspace.org"),
                     "Test remove HTTP");

        assertEquals("dspace.org",
                     Utils.getHostName("https://dspace.org"),
                     "Test remove HTTPS");

        assertEquals("dspace.org",
                     Utils.getHostName("https://dspace.org/"),
                     "Test remove trailing slash");

        assertEquals("dspace.org",
                     Utils.getHostName("https://www.dspace.org"),
                     "Test remove www.");

        assertEquals("demo.dspace.org",
                     Utils.getHostName("https://demo.dspace.org"),
                     "Test keep other prefixes");

        assertEquals("demo.dspace.org",
                     Utils.getHostName("https://demo.dspace.org/search?query=test"),
                     "Test with parameter");

        assertEquals("demo.dspace.org",
                     Utils.getHostName("https://demo.dspace.org/search?query=test turbine"),
                     "Test with parameter with space");

        // This uses a bunch of reserved URI characters
        assertNull(Utils.getHostName("&+,?/@="), "Test invalid URI returns null");
    }

    /**
     * Test of getIPAddresses method, of class Utils
     */
    @Test
    public void testGetIPAddresses() throws UnknownHostException {
        // Fake a URL & two fake corresponding IP addresses as an InetAddress
        String fakeUrl = "https://dspace.org";
        String fakeHostname = "dspace.org";
        InetAddress[] fakeInetAddresses =
            new InetAddress[] { InetAddress.getByName("1.2.3.4"), InetAddress.getByName("5.6.7.8") };

        // Mock responses from InetAddress
        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            // When fakeHostname is passed to InetAddress, return fakeInetAddresses
            mockedInetAddress.when(() -> InetAddress.getAllByName(fakeHostname)).thenReturn(fakeInetAddresses);

            assertNull(Utils.getIPAddresses("not/a-real;url"),
                       "Test invalid URL returns null");

            assertArrayEquals(new String[] {"1.2.3.4", "5.6.7.8"},
                              Utils.getIPAddresses(fakeUrl),
                              "Test fake URL returns fake IPs converted to String Array");
        }
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

        assertEquals(expectedValue,
                     Utils.interpolateConfigsInString(stringWithVariable),
                     "Test config interpolation");

        // remove the config we added
        configurationService.setProperty(configName, null);
    }
}
