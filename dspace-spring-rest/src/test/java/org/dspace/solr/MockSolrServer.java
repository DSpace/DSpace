/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

<<<<<<< HEAD
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
=======
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
>>>>>>> [DS-3695] Upgrade Solr *client* to 7.3.0.
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.dspace.app.rest.test.AbstractDSpaceIntegrationTest;

/**
 * Abstract class to mock a service that uses SOLR
 */
public class MockSolrServer {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MockSolrServer.class);
    private static final ConcurrentMap<String, SolrClient> loadedCores = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, AtomicLong> usersPerCore = new ConcurrentHashMap<>();
    private static CoreContainer container = null;

    private final String coreName;

    private SolrClient solrServer = null;


    public MockSolrServer(final String coreName) throws Exception {
        this.coreName = coreName;
        initSolrServer();
    }

    public SolrClient getSolrServer() {
        return solrServer;
    }

    protected void initSolrServer() throws Exception {
        solrServer = loadedCores.get(coreName);
        if (solrServer == null) {
            solrServer = initSolrServerForCore(coreName);
        }

        usersPerCore.putIfAbsent(coreName, new AtomicLong(0));
        usersPerCore.get(coreName).incrementAndGet();
    }

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
                e.printStackTrace(System.err);
            }

            loadedCores.put(coreName, server);
            log.info("SOLR Server for core " + coreName + " initialized");
        }
        return server;
    }

    public void destroy() throws Exception {
        if (solrServer != null) {
            long remainingUsers = usersPerCore.get(coreName).decrementAndGet();
            if (remainingUsers <= 0) {
                solrServer.close();
                usersPerCore.remove(coreName);
                loadedCores.remove(coreName);
                log.info("SOLR Server for core " + coreName + " destroyed");
            }

            if (usersPerCore.isEmpty()) {
                destroyContainer();
            }
        }
    }

    private static synchronized void initSolrContainer() {
        if (container == null) {
            String solrDir = AbstractDSpaceIntegrationTest.getDspaceDir() + File.separator + "solr";
            log.info("Initializing SOLR CoreContainer with directory " + solrDir);
            container = new CoreContainer(solrDir);
            container.load();
            log.info("SOLR CoreContainer initialized");
        }
    }

    private static synchronized void destroyContainer() {
        container = null;
        log.info("SOLR CoreContainer destroyed");
    }

}
