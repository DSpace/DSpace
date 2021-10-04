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
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Named
@Singleton
public class HttpConnectionPoolService {
    @Inject
    private ConfigurationService configurationService;

    private final PoolingHttpClientConnectionManager connManager
            = new PoolingHttpClientConnectionManager();

    private final ConnectionKeepAliveStrategy keepAliveStrategy
            = new KeepAliveStrategy();

    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 20;

    private static final int DEFAULT_MAX_PER_ROUTE = 15;

    private static final int DEFAULT_KEEPALIVE = 5 * 1000; // milliseconds

    private static final int CHECK_INTERVAL = 1000; // milliseconds

    private static final int IDLE_INTERVAL = 30; // seconds

    @PostConstruct
    protected void init() {
        connManager.setMaxTotal(configurationService.getIntProperty(
                "solrClient.maxTotalConnections", DEFAULT_MAX_TOTAL_CONNECTIONS));
        connManager.setDefaultMaxPerRoute(
                configurationService.getIntProperty("solrClient.maxPerRoute",
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

            return configurationService.getIntProperty("solrClient.keepAlive",
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
        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
