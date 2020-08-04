/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.core.ConfigurationManager;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceSolrServer {
    private static final Logger log = LogManager.getLogger(DSpaceSolrServer.class);

    private static SolrClient _server = null;

    /**
     * Default constructor
     */
    private DSpaceSolrServer() { }

    public static SolrClient getServer() throws SolrServerException {
        if (_server == null) {
            String serverUrl = ConfigurationManager.getProperty("oai.solr.url");
            try {
                _server = new HttpSolrClient.Builder(serverUrl).build();
                log.debug("OAI Solr Server Initialized");
            } catch (Exception e) {
                log.error("Could not initialize OAI Solr Server at " + serverUrl , e);
            }
        }
        return _server;
    }
}
