/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
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

    @Override
    protected boolean isExternalDataObjectPotentiallySuggested(Context context, ExternalDataObject externalDataObject) {
        return StringUtils.equals(MockSuggestionExternalDataSource.NAME, externalDataObject.getSource());
    }
}
