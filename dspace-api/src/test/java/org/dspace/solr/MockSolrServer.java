/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.dspace.AbstractDSpaceIntegrationTest;

/**
 * Factory of connections to an in-process embedded Solr service.
 * Each instance of this class returns connections to a single core.
 * Each connection behaves as would a client connection to a remote service.
 *
 * <p>
 * The service is started as needed.
 * There is at most one open connection to each core; connections are shared and
 * reference-counted.  When the last connection to the last core is closed,
 * the service is shut down.
 *
 * <p>
 * To use this:
 * <ol>
 *   <li>{@code SolrServer mycore = new MockSolrServer("mycore").getSolrServer();}</li>
 *   <li>{@code mycore.this(); mycore.that();}</li>
 *   <li>{@code mycore.destroy();}</li>
 * </ol>
 */
public class MockSolrServer {

    private static final Logger log = LogManager.getLogger();

    /** Shared embedded Solr connections, by name. */
    private static final ConcurrentMap<String, SolrClient> loadedCores = new ConcurrentHashMap<>();

    /** Reference counts for each core. */
    private static final ConcurrentMap<String, AtomicLong> usersPerCore = new ConcurrentHashMap<>();

    /** Container for embedded Solr cores. */
    private static CoreContainer container = null;

    /** Name of this connection's core. */
    private final String coreName;

    /** This instance's connection. */
    private SolrClient solrServer = null;

    /**
     * Wrap an instance of embedded Solr.
     *
     * @param coreName name of the core to serve.
     */
    public MockSolrServer(final String coreName) {
        this.coreName = coreName;
        initSolrServer();
    }

    /**
     * @return the wrapped SolrServer.
     */
    public SolrClient getSolrServer() {
        return solrServer;
    }

    /**
     * Ensure that this instance's core is loaded.  Create it if necessary.
     */
    protected void initSolrServer() {
        solrServer = loadedCores.get(coreName);
        if (solrServer == null) {
            solrServer = initSolrServerForCore(coreName);
        }

        usersPerCore.putIfAbsent(coreName, new AtomicLong(0));
        usersPerCore.get(coreName).incrementAndGet();
    }

    /**
     * Create a SolrServer for this instance's core.  Initialize the container
     * if no cores are loaded.  Delete all records in the core.
     *
     * @param coreName name of the core to "connect".
     * @return connection to the named core.
     */
    private static synchronized SolrClient initSolrServerForCore(final String coreName) {
        SolrClient server = loadedCores.get(coreName);
        if (server == null) {
            initSolrContainer();

            server = new EmbeddedSolrServer(container, coreName);

            //Start with an empty index
            try {
                server.deleteByQuery("*:*");
                server.commit();
            } catch (SolrServerException | IOException e) {
                log.error("Failed to empty Solr index:  {}", e.getMessage(), e);
            }

            loadedCores.put(coreName, server);
            log.info("SOLR Server for core {} initialized", coreName);
        }
        return server;
    }

    /**
     * Remove all records.
     */
    public void reset() {
        try {
            solrServer.deleteByQuery("*:*");
        } catch (SolrServerException | IOException ex) {
            log.warn("Exception while clearing '{}' core", coreName, ex);
        }
    }

    /**
     * Decrease the reference count for connection to the current core.
     * If now zero, shut down the connection and discard it.  If no connections
     * remain, destroy the container.
     *
     * @throws Exception passed through.
     */
    public void destroy() throws Exception {
        if (solrServer != null) {
            long remainingUsers = usersPerCore.get(coreName).decrementAndGet();
            if (remainingUsers <= 0) {
                solrServer.close();
                usersPerCore.remove(coreName);
                loadedCores.remove(coreName);
                log.info("SOLR Server for core {} destroyed", coreName);
            }

            if (usersPerCore.isEmpty()) {
                destroyContainer();
            }
        }
    }

    /**
     * Ensure that a Solr CoreContainer is allocated, aimed at the test Solr
     * home directory (which should contain test cores).
     */
    private static synchronized void initSolrContainer() {
        if (container == null) {
            Path solrDir = Paths.get(AbstractDSpaceIntegrationTest.getDspaceDir(), "solr");
            log.info("Initializing SOLR CoreContainer with directory {}", solrDir.toAbsolutePath().toString());
            container = new CoreContainer(solrDir, new Properties());
            container.load();
            log.info("SOLR CoreContainer initialized");
        }
    }

    /**
     * Discard the embedded Solr container.
     */
    private static synchronized void destroyContainer() {
        container.shutdown();
        container = null;
        log.info("SOLR CoreContainer destroyed");
    }
}
