/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.dspace.solr.MockSolrServer;

/**
 * Factory of EmbeddedSolrClient instances.
 * Wrapper for {@link org.dspace.solr.MockSolrServer}.  Possibly useful for
 * testing.
 *
 * <p>
 * To use this:
 * <ol>
 *   <li>{@code SolrClientFactory scf = new EmbeddedSolrClientFactory();}</li>
 *   <li>{@code SolrClient mycore = scf.getClient("mycore");}</li>
 *   <li>{@code mycore.this(); mycore.that();}</li>
 *   <li>{@code mycore.destroy();}</li>
 * </ol>
 *
 * @author mwood
 */
public class EmbeddedSolrClientFactory
        implements SolrClientFactory {
    private static final Logger log = LogManager.getLogger();

    /** Name of this connection's core. */
    private String coreName;

    /** This instance's connection. */
    private SolrClient solrClient = null;

    private MockSolrServer mockSolrServer;

    @Override
    public SolrClient getClient(String coreUrl) {
        try {
            coreName = Path.of(new URL(coreUrl).getPath())
                    .getFileName()
                    .toString();
        } catch (MalformedURLException ex) {
            log.warn("Unable to extract core name from URI '{}':  {}",
                    coreUrl, ex.getMessage());
        }

        try {
            mockSolrServer = new MockSolrServer(coreName);
            solrClient = mockSolrServer.getSolrServer();
        } catch (Exception ex) {
            log.warn("Failed to instantiate a MockSolrServer", ex);
            solrClient = null;
        }
        return solrClient;
    }

    /**
     * Remove all records.
     */
    public void reset() {
        mockSolrServer.reset();
    }

    /**
     * Decrease the reference count for connection to the current core.
     * If now zero, shut down the connection and discard it.  If no connections
     * remain, destroy the container.
     *
     * @throws Exception passed through.
     */
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
