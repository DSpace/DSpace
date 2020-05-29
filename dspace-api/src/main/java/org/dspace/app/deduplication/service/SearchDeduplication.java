/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service;

import java.util.List;

public class SearchDeduplication {

    private List<SolrDedupServiceIndexPlugin> solrIndexPlugin;

    public List<SolrDedupServiceIndexPlugin> getSolrIndexPlugin() {
        return solrIndexPlugin;
    }

    public void setSolrIndexPlugin(List<SolrDedupServiceIndexPlugin> solrIndexPlugin) {
        this.solrIndexPlugin = solrIndexPlugin;
    }

}
