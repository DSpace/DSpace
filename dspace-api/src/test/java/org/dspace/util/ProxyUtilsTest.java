package org.dspace.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

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
}
