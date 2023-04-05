/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Service
public class MockSolrStatisticsCore extends SolrStatisticsCore implements DisposableBean {

    private MockSolrServer mockSolrServer = new MockSolrServer("statistics");

    @Override
    public void initSolr() {
        if (solr == null) {
            solr = mockSolrServer.getSolrServer();
        }
    }

    /**
     * Reset the core for the next test.  See {@link MockSolrServer#reset()}.
     */
    public void reset() {
        mockSolrServer.reset();
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
