/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final ConcurrentMap<String, SolrClient> loadedCores
            = new ConcurrentHashMap<>();

    /** Reference counts for each core. */
    private static final ConcurrentMap<String, AtomicLong> usersPerCore
            = new ConcurrentHashMap<>();

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
    private void initSolrServer() {
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
        log.info("DEBUG: initSolrServerForCore called for core '{}'", coreName);
        SolrClient server = loadedCores.get(coreName);
        if (server == null) {
            log.info("DEBUG: Core '{}' not in cache, initializing...", coreName);
            initSolrContainer();

            log.info("DEBUG: Container initialized, checking if core '{}' exists...", coreName);

            // Check if the requested core is available
            if (container != null) {
                Collection<String> availableCores = container.getAllCoreNames();
                log.info("DEBUG: Available cores: {}", availableCores);
                if (!availableCores.contains(coreName)) {
                    log.warn("DEBUG: Requested core '{}' is NOT in available cores list!", coreName);
                }
            }

            try {
                log.info("DEBUG: Creating EmbeddedSolrServer for core '{}'...", coreName);
                server = new EmbeddedSolrServer(container, coreName) {
                    // This ugliness should be fixed in Solr 8.9.
                    // https://issues.apache.org/jira/browse/SOLR-15085
                    @Override public void close() { // Copied from Solr's own tests
                        // Do not close shared core container!
                    }
                };
                log.info("DEBUG: EmbeddedSolrServer created successfully for core '{}'", coreName);
            } catch (Exception e) {
                log.error("DEBUG: Failed to create EmbeddedSolrServer for core '{}'", coreName, e);
                throw new RuntimeException("Failed to create EmbeddedSolrServer for core: " + coreName, e);
            }

            //Start with an empty index
            try {
                log.info("DEBUG: Clearing index for core '{}'...", coreName);
                server.deleteByQuery("*:*");
                server.commit();
                log.info("DEBUG: Index cleared successfully for core '{}'", coreName);
            } catch (SolrServerException | IOException e) {
                log.error("DEBUG: Failed to empty Solr index for core '{}': {}", coreName, e.getMessage(), e);
            }

            loadedCores.put(coreName, server);
            log.info("SOLR Server for core {} initialized", coreName);
        } else {
            log.info("DEBUG: Core '{}' already in cache, reusing", coreName);
        }
        return server;
    }

    /**
     * Remove all records.
     */
    public void reset() {
        if (null == solrServer) {
            log.warn("reset called with no server connection");
            return;
        }

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
            String dspaceDir = AbstractDSpaceIntegrationTest.getDspaceDir();
            log.info("DEBUG: dspace.dir system property = {}", dspaceDir);

            if (dspaceDir == null) {
                log.error("DEBUG: dspace.dir is NULL! This will cause Solr initialization to fail.");
                throw new IllegalStateException("dspace.dir system property is not set");
            }

            Path solrDir = Path.of(dspaceDir, "solr");
            log.info("DEBUG: Initializing SOLR CoreContainer with directory {}",
                    solrDir.toAbsolutePath().toString());

            // Check if directory exists and list contents
            if (Files.exists(solrDir)) {
                log.info("DEBUG: Solr directory exists: {}", solrDir);
                try (Stream<Path> files = Files.list(solrDir)) {
                    String contents = files.map(p -> p.getFileName().toString())
                                           .collect(Collectors.joining(", "));
                    log.info("DEBUG: Solr directory contents: [{}]", contents);
                } catch (IOException e) {
                    log.warn("DEBUG: Could not list solr directory contents", e);
                }

                // Check for solr.xml
                Path solrXml = solrDir.resolve("solr.xml");
                if (Files.exists(solrXml)) {
                    log.info("DEBUG: solr.xml exists at {}", solrXml);
                    try {
                        String xmlContent = Files.readString(solrXml);
                        log.info("DEBUG: solr.xml content: {}", xmlContent);
                    } catch (IOException e) {
                        log.warn("DEBUG: Could not read solr.xml", e);
                    }
                } else {
                    log.error("DEBUG: solr.xml NOT FOUND at {}", solrXml);
                }
            } else {
                log.error("DEBUG: Solr directory does NOT exist: {}", solrDir);
            }

            try {
                log.info("DEBUG: Creating CoreContainer...");
                container = new CoreContainer(solrDir, new Properties());
                log.info("DEBUG: CoreContainer created, calling load()...");
                container.load();
                log.info("DEBUG: CoreContainer.load() completed");

                // Log container status after load
                log.info("DEBUG: Container status - isShutDown: {}", container.isShutDown());

                // Try to get available cores
                Collection<String> coreNames = container.getAllCoreNames();
                log.info("DEBUG: Available cores after load: {}", coreNames);

                if (coreNames.isEmpty()) {
                    log.warn("DEBUG: No cores found after container.load()! This may cause issues.");
                }
            } catch (Exception e) {
                log.error("DEBUG: Exception during CoreContainer initialization", e);
                throw new RuntimeException("Failed to initialize Solr CoreContainer", e);
            }

            log.info("SOLR CoreContainer initialized successfully");
        } else {
            log.info("DEBUG: CoreContainer already initialized, reusing existing instance");
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
