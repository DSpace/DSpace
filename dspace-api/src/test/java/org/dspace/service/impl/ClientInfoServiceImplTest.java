/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.util.DummyHttpServletRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test class for the {@link ClientInfoServiceImpl} class which implements
 * the {@link ClientInfoService} interface
 *
 * @author tom dot desair at gmail dot com
 */
public class ClientInfoServiceImplTest extends AbstractDSpaceTest {

    private ClientInfoService clientInfoService;

    private ConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Test
    public void getClientIpFromRequest() {
        configurationService.setProperty("useProxies", true);
        configurationService.setProperty("proxies.trusted.ipranges", "127.0.0");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        DummyHttpServletRequest req = new DummyHttpServletRequest();
        req.setAddress("127.0.0.1");
        req.addHeader("X-Forwarded-For", "192.168.1.24");

        assertEquals("192.168.1.24", clientInfoService.getClientIp(req));
    }

    @Test
    public void getClientIpWithTrustedProxy() {
        configurationService.setProperty("useProxies", true);
        configurationService.setProperty("proxies.trusted.ipranges", "127.0.0");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        String remoteIp = "127.0.0.1";
        String xForwardedFor = "192.168.1.24";

        assertEquals("192.168.1.24",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));
    }


    @Test
    public void getClientIpWithUntrustedProxy() {
        configurationService.setProperty("useProxies", true);
        configurationService.setProperty("proxies.trusted.ipranges", "192.168.1.1");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        String remoteIp = "10.24.64.14";
        String xForwardedFor = "192.168.1.24";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));
    }

    @Test
    public void getClientIpWithMultipleTrustedProxies() {
        configurationService.setProperty("useProxies", true);
        configurationService.setProperty("proxies.trusted.ipranges", "127.0.0,192.168.1");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        String remoteIp = "127.0.0.1";
        String xForwardedFor = "10.24.64.14,192.168.1.24";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));

        xForwardedFor = "192.168.1.24,10.24.64.14";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));
    }

    @Test
    public void getClientIpWithoutTrustedProxies() {
        configurationService.setProperty("useProxies", true);
        configurationService.setProperty("proxies.trusted.ipranges", "");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        String remoteIp = "127.0.0.1";
        String xForwardedFor = "10.24.64.14";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));

        xForwardedFor = "127.0.0.1,10.24.64.14";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));

        xForwardedFor = "10.24.64.14,127.0.0.1";

        assertEquals("10.24.64.14",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));
    }

    @Test
    public void getClientIpWithoutUseProxies() {
        configurationService.setProperty("useProxies", false);
        configurationService.setProperty("proxies.trusted.ipranges", "");

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        String remoteIp = "127.0.0.1";
        String xForwardedFor = "10.24.64.14";

        assertEquals("127.0.0.1",
                clientInfoService.getClientIp(remoteIp, xForwardedFor));

        assertEquals("127.0.0.1",
                clientInfoService.getClientIp(remoteIp, null));

        assertEquals("127.0.0.1",
                clientInfoService.getClientIp(remoteIp, ""));
    }

    @Test
    public void isUseProxiesEnabledTrue() {
        configurationService.setProperty("useProxies", true);

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        assertTrue(clientInfoService.isUseProxiesEnabled());
    }

    @Test
    public void isUseProxiesEnabledFalse() {
        configurationService.setProperty("useProxies", false);

        clientInfoService = new ClientInfoServiceImpl(configurationService);

        assertFalse(clientInfoService.isUseProxiesEnabled());
    }
}
