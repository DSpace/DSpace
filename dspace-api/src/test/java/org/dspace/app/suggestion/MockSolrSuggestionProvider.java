package org.dspace.app.suggestion;

import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class MockSolrSuggestionProvider extends SolrSuggestionProvider implements InitializingBean, DisposableBean {
    private MockSolrServer mockSolrServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("suggestion");
        solrSuggestionClient = mockSolrServer.getSolrServer();
    }

    /** Clear all records from the search core. */
    public void reset() {
        mockSolrServer.reset();
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
