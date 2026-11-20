/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.util.SolrAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory of HtmlSolrClient instances.
 *
 * @author mwood
 */
public class HttpSolrClientFactory
        implements SolrClientFactory {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public SolrClient getClient(String coreUrl) {
        CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build(builder ->
                SolrAuthUtils.addAuthenticationIfConfigured(builder, "solr", configurationService));
        SolrClient client = new HttpSolrClient.Builder()
                .withBaseSolrUrl(coreUrl)
                .withHttpClient(httpClient)
                .build();
        return client;
    }
}
