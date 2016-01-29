/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.dspace.discovery.SearchServiceException;

public class VisualizationGraphIndexer {

	private static Logger log = Logger
			.getLogger(VisualizationGraphIndexer.class);

	private HttpSolrServer solr;

	private CrisSearchService indexer;
	
	public HttpSolrServer getSolr() {
		init();
		return solr;
	}

	private synchronized void init() {
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
		 

	/**
	 * Method to index a Researcher Grant.
	 * 
	 * @param rp
	 *            the researcher page
	 * @throws IOException
	 * @throws SolrServerException
	 * @throws NoSuchAlgorithmException 
	 * @throws SearchServiceException
	 */
	public void index(List<VisualizationGraphNode> nodes) throws SolrServerException,
			IOException, NoSuchAlgorithmException {

		List<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>(nodes.size());
		
		for (VisualizationGraphNode node : nodes) {
			SolrInputDocument doc1 = new SolrInputDocument();
			// Save our basic info that we already have
		
			doc1.addField("type", node.getType());
			
			doc1.addField("a", node.getA());
			doc1.addField("b", node.getB());
			
			doc1.addField("a_auth", node.getA_auth());		
			doc1.addField("b_auth", node.getB_auth());
			
			doc1.addField("a_dept", node.getA_dept());        
	        doc1.addField("b_dept", node.getB_dept());
	        doc1.addField("focus_dept", node.getA_dept()+"|||"+node.getB_dept());
	        
			doc1.addField("a_val", node.getFavalue());		
			doc1.addField("b_val", node.getFbvalue());
			
			doc1.addField("value", node.getValue());
			doc1.addField("entity", node.getEntity());
			doc1.addField("extra", node.getExtra());
			
			log.debug("add document on solr index " + node.getA() + "|||" + node.getB());
			solrDocs.add(doc1);
			if (solrDocs.size() % 500 == 0) {
				getSolr().add(solrDocs);
				solrDocs.clear();
			}
		}
		try {
			getSolr().add(solrDocs);
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void removeIndex(VisualizationGraphNode node) throws IOException,
			SolrServerException {
		removeIndex(node.getA_auth(), node.getB_auth(), node.getType());
	}

	public void removeIndex(String node1, String node2, String type)
			throws SolrServerException, IOException {
		getSolr().deleteByQuery(
				"node1:" + node1 + " AND node2:" + node2 + " AND type:" + type);
		getSolr().commit();
	}

	/**
	 * create full index - wiping old index
	 * 
	 * @param c
	 *            context to use
	 * @throws Exception
	 */
	public void createIndex(String connection) throws Exception {

		updateIndex(connection);

	}

	/**
	 * Iterates over all Visualizations and updates them in the index. Uses
	 * indexContent and isStale to check state of item in index.
	 * 
	 * @param context
	 * @throws Exception
	 */
	public void updateIndex(String connection) throws Exception {
		NetworkPlugin plugin = (NetworkPlugin) PluginManager.getNamedPlugin(
				NetworkPlugin.class, connection);
		List<String[]> discardedNode = new LinkedList<String[]>();		
		Integer importedNodes = 0;
		Boolean otherError = false;
		updateIndex(plugin.load(discardedNode, importedNodes, otherError));
	}

	/**
	 * Iterates over all ResearcherPages and updates them in the index. Uses
	 * indexContent and isStale to check state of item in index.
	 * <p/>
	 * 
	 * @param context
	 * @param force
	 */
	public void updateIndex(List<VisualizationGraphNode> nodes) {
		try {
				index(nodes);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Delete all index
	 * 
	 * @throws SolrServerException
	 */
	public void cleanIndex() throws IOException, SolrServerException {

		getSolr().deleteByQuery("*:*");
		getSolr().commit();

	}

	public void commit() {
		try {
			getSolr().commit();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void optimize() {
		try {
			getSolr().optimize();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

    public void setIndexer(CrisSearchService indexerService)
    {
        this.indexer = indexerService;
    }

    public CrisSearchService getIndexer()
    {
        return indexer;
    }

}
