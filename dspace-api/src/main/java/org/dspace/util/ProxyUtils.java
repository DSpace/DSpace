/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for HTTP proxies.
 *
 * @author mwood
 */
public class ProxyUtils {
    public static final String C_HTTP_PROXY_HOST = "http.proxy.host";
    public static final String C_HTTP_PROXY_PORT = "http.proxy.port";
    public static final int D_HTTP_PROXY_PORT = -1;

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private ProxyUtils() {}

    /**
     * Add a proxy to an HttpClientBuilder if a proxy is configured.
     * See configuration properties {@code http.proxy.host}, {@code http.proxy.port}.
     * If host is set and port is not, use the default port for the scheme.  If
     * host is not set, return the builder unaltered.
     *
     * @param builder the builder to be configured.
     * @return the builder, possibly configured with a proxy.
     */
    public static HttpClientBuilder addProxy(HttpClientBuilder builder) {
        String proxyHost = configurationService.getProperty(C_HTTP_PROXY_HOST);
        if (null != proxyHost) {
            int proxyPort = configurationService.getIntProperty(C_HTTP_PROXY_PORT,
                    D_HTTP_PROXY_PORT);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            builder.setProxy(proxy);
        }
        return builder;
    }

    /**
     * Create a {@link Proxy} instance from our configuration for a
     * {@link java.net.URLConnection}, if configured.
     *
     * @return a configured Proxy, or {@link Proxy.NO_PROXY} if unconfigured.
     */
    public static Proxy getProxy() {
        Proxy proxy = Proxy.NO_PROXY;
        String proxyHost = configurationService.getProperty(C_HTTP_PROXY_HOST);
        if (null != proxyHost) {
            int proxyPort = configurationService.getIntProperty(C_HTTP_PROXY_PORT);
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }
        return proxy;
    }
}
