/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service.impl;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.dspace.services.ConfigurationService;

/**
 * Factory for HTTP clients sharing a pool of connections.
 *
 * <p>You may create multiple pools.  Each is identified by a configuration
 * "prefix" (passed to the constructor) which is used to create names of
 * properties which will configure the pool.  The properties are:
 *
 * <dl>
 *   <dt>PREFIX.client.keepAlive</dt>
 *   <dd>Default keep-alive time for open connections, in milliseconds</dd>
 *   <dt>PREFIX.client.maxTotalConnections</dt>
 *   <dd>maximum open connections</dd>
 *   <dt>PREFIX.client.maxPerRoute</dt>
 *   <dd>maximum open connections per service instance</dd>
 *   <dt>PREFIX.client.timeToLive</dt>
 *   <dd>maximum lifetime of a pooled connection, in seconds</dd>
 * </dl>
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Named
@Singleton
public class HttpConnectionPoolService {
    @Inject
    ConfigurationService configurationService;

    /** Configuration properties will begin with this string. */
    private final String configPrefix;

    /** Maximum number of concurrent pooled connections. */
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 20;

    /** Maximum number of concurrent pooled connections per route. */
    private static final int DEFAULT_MAX_PER_ROUTE = 15;

    /** Keep connections open at least this long, if the response did not
     *  specify:  milliseconds
     */
    private static final int DEFAULT_KEEPALIVE = 5 * 1000;

    /** Pooled connection maximum lifetime:  seconds */
    private static final int DEFAULT_TTL = 10 * 60;

    /** Clean up stale connections this often:  milliseconds */
    private static final int CHECK_INTERVAL = 1000;

    /** Connection idle if unused for this long:  seconds */
    private static final int IDLE_INTERVAL = 30;

    private PoolingHttpClientConnectionManager connManager;

    private final ConnectionKeepAliveStrategy keepAliveStrategy
            = new KeepAliveStrategy();

    /**
     * Construct a pool for a given set of configuration properties.
     *
     * @param configPrefix Configuration property names will begin with this.
     */
    public HttpConnectionPoolService(String configPrefix) {
        this.configPrefix = configPrefix;
    }

    @PostConstruct
    protected void init() {
        connManager = new PoolingHttpClientConnectionManager(
                configurationService.getIntProperty(configPrefix + ".client.timeToLive", DEFAULT_TTL),
                TimeUnit.SECONDS);

        connManager.setMaxTotal(configurationService.getIntProperty(
                configPrefix + ".client.maxTotalConnections", DEFAULT_MAX_TOTAL_CONNECTIONS));
        connManager.setDefaultMaxPerRoute(
                configurationService.getIntProperty(configPrefix + ".client.maxPerRoute",
                        DEFAULT_MAX_PER_ROUTE));

        Thread connectionMonitor = new IdleConnectionMonitorThread(connManager);
        connectionMonitor.setDaemon(true);
        connectionMonitor.start();
    }

    /**
     * Create an HTTP client which uses a pooled connection.
     *
     * @return the client.
     */
    public CloseableHttpClient getClient() {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setKeepAliveStrategy(keepAliveStrategy)
                .setConnectionManager(connManager)
                .build();
        return httpClient;
    }

    /**
     * A connection keep-alive strategy that obeys the Keep-Alive header and
     * applies a default if none is given.
     *
     * Swiped from https://www.baeldung.com/httpclient-connection-management
     */
    public class KeepAliveStrategy
            implements ConnectionKeepAliveStrategy {
        @Override
        public long getKeepAliveDuration(HttpResponse response,
                HttpContext context) {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String name = he.getName();
                String value = he.getValue();
                if (value != null && "timeout".equalsIgnoreCase(name)) {
                    return Long.parseLong(value) * 1000;
                }
            }

            // If server did not request keep-alive, use configured value.
            return configurationService.getIntProperty(configPrefix + ".client.keepAlive",
                    DEFAULT_KEEPALIVE);
        }
    }

    /**
     * Clean up stale connections.
     *
     * Swiped from https://www.baeldung.com/httpclient-connection-management
     */
    public class IdleConnectionMonitorThread
            extends Thread {
        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        /**
         * Constructor.
         *
         * @param connMgr the manager to be monitored.
         */
        public IdleConnectionMonitorThread(
            PoolingHttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(CHECK_INTERVAL);
                        connMgr.closeExpiredConnections();
                        connMgr.closeIdleConnections(IDLE_INTERVAL, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                shutdown();
            }
        }

        /**
         * Cause a controlled exit from the thread.
         */
        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
