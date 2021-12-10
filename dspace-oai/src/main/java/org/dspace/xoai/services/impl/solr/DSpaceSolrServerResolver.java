/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.solr;

import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceSolrServerResolver implements SolrServerResolver {
    private static final Logger log = LogManager.getLogger(DSpaceSolrServerResolver.class);
    private static SolrClient server = null;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired @Named("solrHttpConnectionPoolService")
    private HttpConnectionPoolService httpConnectionPoolService;

    @Override
    public SolrClient getServer() throws SolrServerException {
        if (server == null) {
            String serverUrl = configurationService.getProperty("oai.solr.url");
            try {
                server = new HttpSolrClient.Builder(serverUrl)
                        .withHttpClient(httpConnectionPoolService.getClient())
                        .build();
                log.debug("OAI Solr Server Initialized");
            } catch (Exception e) {
                log.error("Could not initialize OAI Solr Server at " + serverUrl , e);
            }
        }
        return server;
    }
}
