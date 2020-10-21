/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.solr;

import org.dspace.solr.MockSolrServer;
import org.dspace.statistics.SolrStatisticsCore;
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

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
