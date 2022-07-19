/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service;

import java.util.List;

/**
 * Service to provide access to configured Solr deduplication plugins
 * See the deduplication.xml spring configuration for plugin bean definitions and configuration
 *
 * @author 4Science
 */
public class DeduplicationPluginService {

    // Solr index plugin list
    private List<SolrDedupServiceIndexPlugin> solrIndexPlugin;

    /**
     * Get list of configured Solr index plugins
     * @return  list of plugins
     */
    public List<SolrDedupServiceIndexPlugin> getSolrIndexPlugin() {
        return solrIndexPlugin;
    }

    /**
     * Set list of configured Solr index plugins
     * @param solrIndexPlugin   list of plugins
     */
    public void setSolrIndexPlugin(List<SolrDedupServiceIndexPlugin> solrIndexPlugin) {
        this.solrIndexPlugin = solrIndexPlugin;
    }

}
