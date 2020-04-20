/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication;

import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Mock SOLR service for the Dedup Core.
 */
@Service
public class MockSolrDedupCore extends SolrDedupServiceImpl implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
