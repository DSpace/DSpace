/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;

/**
 * Factory of HtmlSolrClient instances.
 *
 * @author mwood
 */
public class HttpSolrClientFactory
        implements SolrClientFactory {

    @Override
    public SolrClient getClient(String coreUrl) {
        SolrClient client = new HttpJdkSolrClient.Builder(coreUrl)
                .build();
        return client;
    }
}
