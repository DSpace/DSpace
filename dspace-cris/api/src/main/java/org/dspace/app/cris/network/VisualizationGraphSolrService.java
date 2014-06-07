/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchServiceException;

public class VisualizationGraphSolrService {
	
	private static Logger log = Logger.getLogger(VisualizationGraphSolrService.class);
	

    private CrisSearchService searcher;

	/**
     * HttpSolrServer for processing solr events.
     */
    private static HttpSolrServer solr = null;

    
    private static void init() {
		if (solr != null)
			return;

		String serverProperty = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE,"network.server");
        log.info("network.server:"
				+ serverProperty);

		HttpSolrServer server = null;

		if (serverProperty != null) {
			try {
				server = new HttpSolrServer(
				        serverProperty);


			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		solr = server;
	}

    //******** SearchService implementation
    public QueryResponse search(SolrQuery query) throws SearchServiceException {
        try {
            return getSolr().query(query);
        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(),e);
        }
    }

    
	public static HttpSolrServer getSolr() {
		init();
		return solr;
	}

	public static void setSolr(HttpSolrServer solr) {
		VisualizationGraphSolrService.solr = solr;
	}

	public CrisSearchService getSearcher() {
		return searcher;
	}

	public void setSearcher(CrisSearchService searcher) {
		this.searcher = searcher;
	}

}
