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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for HTTP proxies.
 * See configuration properties {@code http.proxy.host}, {@code http.proxy.port}.
 * If host is set and port is not, or if host is not set, then proxying is
 * disabled.
 *
 * @author mwood
 */
public class ProxyUtils {
    public static final String C_HTTP_PROXY_HOST = "http.proxy.host";
    public static final String C_HTTP_PROXY_PORT = "http.proxy.port";

    private static final Logger LOG = LogManager.getLogger();
    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private ProxyUtils() {}

    /**
     * Add a proxy to an HttpClientBuilder if a proxy is configured.
     *
     * @param builder the builder to be configured.
     * @return the builder, possibly configured with a proxy.
     */
    public static HttpClientBuilder addProxy(HttpClientBuilder builder) {
        String proxyHost = configurationService.getProperty(C_HTTP_PROXY_HOST);
        if (StringUtils.isNotBlank(proxyHost)) {
            int proxyPort = getPort();
            if (proxyPort >= 0) {
                HttpHost proxy = new HttpHost(proxyHost, proxyPort);
                builder.setProxy(proxy);
            } else {
                LOG.warn(C_HTTP_PROXY_HOST + " is set but " + C_HTTP_PROXY_PORT
                        + " is not.  Proxy disabled.");
            }
        }
        return builder;
    }

    /**
     * Create a {@link Proxy} instance from our configuration for a
     * {@link java.net.URLConnection}, if configured.
     *
     * @return a configured Proxy, or {@link Proxy.NO_PROXY} if not configured.
     */
    public static Proxy getProxy() {
        Proxy proxy = Proxy.NO_PROXY;
        String proxyHost = configurationService.getProperty(C_HTTP_PROXY_HOST);
        if (StringUtils.isNotBlank(proxyHost)) {
            int proxyPort = getPort();
            if (proxyPort >= 0) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            } else {
                LOG.warn(C_HTTP_PROXY_HOST + " is set but " + C_HTTP_PROXY_PORT
                        + " is not.  Proxy disabled.");
            }
        }
        return proxy;
    }

    /**
     * Get the configured proxy port.  Return a default if not configured or not
     * a positive number.
     *
     * @return the port number, or -1 if none.
     */
    private static int getPort() {
        String proxyPort = configurationService.getProperty(C_HTTP_PROXY_PORT);
        if (StringUtils.isNumeric(proxyPort)) {
            return Integer.parseUnsignedInt(proxyPort);
        } else {
            if (configurationService.hasProperty(C_HTTP_PROXY_PORT)) {
                LOG.warn("Proxy port setting " + C_HTTP_PROXY_PORT +
                        " = '{}' is not a positive number.  Treating as unset",
                        proxyPort);
            }
            return -1;
        }
    }
}
