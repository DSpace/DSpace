/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Mock SOLR service for the Search Core
 */
@Service
public class MockSolrServiceImpl extends SolrServiceImpl implements InitializingBean, DisposableBean {

    private MockSolrServer mockSolrServer;

    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("search");
        solr = mockSolrServer.getSolrServer();
    }

    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
