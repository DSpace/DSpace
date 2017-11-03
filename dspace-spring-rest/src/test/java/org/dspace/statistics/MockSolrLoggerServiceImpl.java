/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.dspace.services.ConfigurationService;
import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mock service that uses an embedded SOLR server for the statistics core
 */
public class MockSolrLoggerServiceImpl extends SolrLoggerServiceImpl implements InitializingBean, DisposableBean {

    private MockSolrServer mockSolrServer;

    @Autowired(required = true)
    private ConfigurationService configurationService;

    @Override
    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("statistics");
        solr = mockSolrServer.getSolrServer();
        locationService = new MockLookupService();
        useProxies = configurationService.getBooleanProperty("useProxies");
    }

    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }

}
