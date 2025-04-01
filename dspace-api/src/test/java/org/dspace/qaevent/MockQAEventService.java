/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.qaevent.service.impl.QAEventServiceImpl;
import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Mock SOLR service for the qaevents Core.
 */
@Service
public class MockQAEventService extends QAEventServiceImpl implements InitializingBean, DisposableBean {
    private MockSolrServer mockSolrServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("qaevent");
        solr = mockSolrServer.getSolrServer();
    }

    /** Clear all records from the search core. */
    public void reset() {
        mockSolrServer.reset();
        try {
            mockSolrServer.getSolrServer().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}