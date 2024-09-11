/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class ProxyUtilsTest
        extends AbstractUnitTest {
    private static ConfigurationService cfg;

    @BeforeClass
    public static void initme() {
        cfg = new DSpace().getConfigurationService();
    }

    private static final String PROXY_HOST = "proxy.example.com";
    private static final String PROXY_IP_ADDRESS = "192.168.1.1";
    private static final int PROXY_PORT = 8888;

    /**
     * Test of addProxy method when no proxy is set.
     */
    @Test
    public void testAddProxyNone() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        HttpClientBuilder builderSpy = spy(builder);

        // Test no proxy
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_HOST, null);
        builderSpy = ProxyUtils.addProxy(builderSpy);
        verify(builderSpy,never()).setProxy(any(HttpHost.class));
    }

    /**
     * Test of addProxy method when only host is set.
     */
//    @Ignore
    @Test
    public void testAddProxyNoPort() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        HttpClientBuilder builderSpy = spy(builder);
        HttpHost hostSpy = mock(HttpHost.class,
                withSettings().useConstructor("bogus." + PROXY_HOST, Integer.valueOf(-1)));

        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_HOST, PROXY_HOST);
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_PORT, null);
        builderSpy = ProxyUtils.addProxy(builderSpy);
        verify(builderSpy,times(1)).setProxy(any(HttpHost.class));
        // FIXME this fails with null host name.  How to get at the HttpHost?
        // FIXME assertEquals("Wrong proxy host", PROXY_HOST, hostSpy.getHostName());
        // FIXME assertEquals("Wrong proxy port", PROXY_PORT, hostSpy.getPort()); // FIXME nope?
    }

    /**
     * Test of addProxy method when host and port are set.
     */
    @Test
    public void testAddProxyWithPort() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        HttpClientBuilder builderSpy = spy(builder);
        HttpHost hostSpy = mock(HttpHost.class,
                withSettings().useConstructor("bogus." + PROXY_HOST, PROXY_PORT));

        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_HOST, PROXY_HOST);
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_PORT, PROXY_PORT);
        builderSpy = ProxyUtils.addProxy(builderSpy);
        verify(builderSpy,times(1)).setProxy(any(HttpHost.class));
        // FIXME assertEquals("Wrong proxy host", PROXY_HOST, hostSpy.getHostName());
        // FIXME assertEquals("Wrong proxy port", PROXY_PORT, hostSpy.getPort());
    }

    @Test
    public void TestGetProxy() {
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_HOST, PROXY_IP_ADDRESS);
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_PORT, PROXY_PORT);
        Proxy proxy = ProxyUtils.getProxy();
        Proxy expected = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(PROXY_IP_ADDRESS, PROXY_PORT));
        assertEquals("Wrong proxy", expected, proxy);
    }

    @Test
    public void TestGetProxyNone() {
        cfg.setProperty(ProxyUtils.C_HTTP_PROXY_HOST, null);
        Proxy proxy = ProxyUtils.getProxy();
        Proxy expected = Proxy.NO_PROXY;
        assertEquals("Wrong proxy", expected, proxy);
    }
}
