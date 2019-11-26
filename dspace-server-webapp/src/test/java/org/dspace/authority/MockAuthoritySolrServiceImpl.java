/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Mock SOLR service for the Search Core.
 *
 * <p>
 * <strong>NOTE:</strong>  this class overrides one <em>of the same name</em>
 * defined in dspace-api and declared as a bean there.
 * See {@code config/spring/api/Z-mock-services.xml}.  Some kind of classpath
 * magic makes this work.
 */
@Service
public class MockAuthoritySolrServiceImpl extends AuthoritySolrServiceImpl implements InitializingBean, DisposableBean {

    private MockSolrServer mockSolrServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("authority");
        solr = mockSolrServer.getSolrServer();
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
