package org.dspace.app.cris.deduplication.service;

import java.util.List;

public class SearchDeduplication {

    private List<SolrDedupServiceIndexPlugin> solrIndexPlugin;

    public List<SolrDedupServiceIndexPlugin> getSolrIndexPlugin()
    {
        return solrIndexPlugin;
    }

    public void setSolrIndexPlugin(List<SolrDedupServiceIndexPlugin> solrIndexPlugin)
    {
        this.solrIndexPlugin = solrIndexPlugin;
    }

    
}
