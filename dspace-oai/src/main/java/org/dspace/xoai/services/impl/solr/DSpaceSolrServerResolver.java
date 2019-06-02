/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceSolrServerResolver implements SolrServerResolver {
    private static final Logger log = LogManager.getLogger(DSpaceSolrServerResolver.class);
    private static SolrClient server = null;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public SolrClient getServer() throws SolrServerException {
        if (server == null) {
            try {
                server = new HttpSolrClient.Builder(configurationService.getProperty("oai", "solr.url")).build();
                log.debug("Solr Server Initialized");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return server;
    }
}
