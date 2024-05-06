/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Build connections to Solr cores.
 *
 * @author mwood
 */
public interface SolrClientFactory {
    /**
     * Instantiate a SolrClient connected to a specified core.
     *
     * @param coreUrl URL of the core to connect with.
     * @return a connection to the given core.
     */
    public SolrClient getClient(String coreUrl);
}
