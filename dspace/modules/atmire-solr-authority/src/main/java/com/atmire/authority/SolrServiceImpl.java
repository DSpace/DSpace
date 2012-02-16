package com.atmire.authority;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 7-dec-2010
 * Time: 10:10:19
 * Modified : Fabio March/2011
 */
public class SolrServiceImpl implements IndexingService, SearchService, SolrDocumentFields{

    private static final Logger log = Logger.getLogger(SolrServiceImpl.class);


    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private CommonsHttpSolrServer solr = null;


    /**
     * Non-Static Singleton instance of Configuration Service
     */
    private ConfigurationService configurationService;

    protected CommonsHttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        if ( solr == null)
        {
            //TODO: in dspace 1.7 this can be used with modules
            String solrService = ConfigurationManager.getProperty("solr.authority.server");

            log.debug("Solr authority URL: " + solrService);

            solr = new CommonsHttpSolrServer(solrService);
            solr.setBaseURL(solrService);

            //TODO: a better query perhaps ?
            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");

            solr.query(solrQuery);
        }

        return solr;
    }

    public void indexContent(Map<String,String> values, boolean force) {
        SolrInputDocument doc = buildDocument(values);

        try{
            writeDocument(doc);
        }catch (Exception e){
            e.printStackTrace(System.out);
            log.error("Error while writing authority value to the index: " + e);
        }
    }

    public void cleanIndex() throws Exception {
        try{
            getSolr().deleteByQuery("*:*");
        } catch (Exception e){
            e.printStackTrace(System.out);
            log.error("Error while cleaning authority solr server index", e);
            throw new Exception(e);
        }
    }

    public void cleanIndex(String source) throws Exception {
        try{
            getSolr().deleteByQuery(DOC_SOURCE + ":" + source);
        } catch (Exception e){
            e.printStackTrace(System.out);
            log.error("Error while cleaning authority solr server index", e);
            throw new Exception(e);
        }
    }


    public void commit() {
        try {
            solr.commit();
        } catch (SolrServerException e) {
            e.printStackTrace(System.out);
            log.error("Error while committing authority solr server", e);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            log.error("Error while committing authority solr server", e);
        }
    }


    private SolrInputDocument buildDocument(Map<String, String> values){
        SolrInputDocument doc = new SolrInputDocument();

        System.out.println("<doc>");

        Set<String> keys = values.keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            doc.setField(key, values.get(key)); 

            System.out.println("<" + key + ">" + values.get(key) + "</" + key + ">");
        }       
        System.out.println("</doc>");
        return doc;

    }    

    /**
     * Write the document to the solr index
     * @param doc the solr document
     * @throws java.io.IOException
     */
    private void writeDocument(SolrInputDocument doc) throws IOException {

        try {           
            getSolr().add(doc);
        } catch (SolrServerException e) {
            e.printStackTrace(System.out);
            log.error(e.getMessage(), e);
        }
    }

    public QueryResponse search(SolrQuery query) throws Exception {
        return getSolr().query(query);
    }
}
