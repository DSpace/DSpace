/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * This class has been created so that we can mock our AuthoritySolrSeviceImpl
 */
@Service
public class MockAuthoritySolrServiceImpl extends AuthoritySolrServiceImpl implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        //We don't use SOLR in the tests of this module
        solr = null;
    }
}
