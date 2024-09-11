package org.dspace.util;

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

    /**
     * Add a proxy to the builder if one is configured.
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
}
