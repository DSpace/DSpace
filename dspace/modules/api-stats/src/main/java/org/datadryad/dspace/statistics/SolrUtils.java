/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datadryad.dspace.statistics;

import java.net.MalformedURLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;

/**
 *
 * @author Nathan Day
 */
public class SolrUtils {
    
    private static final Logger LOGGER = Logger.getLogger(SolrUtils.class);
    
    public static final String solrSearchUrlBase = ConfigurationManager.getProperty("solr.search.server");
    public static final String solrStatsUrlBase = ConfigurationManager.getProperty("solr.stats.server");
    
    public static String getSolrXPathResult(String searchUrlBase, String queryUrl, String resultPath) {
        String result = "";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            GetMethod get = new GetMethod(searchUrlBase + queryUrl);

            switch (new HttpClient().executeMethod(get)) {
                case 200:
                case 201:
                case 202:
                    Document doc = db.parse(get.getResponseBodyAsStream());
                    doc.getDocumentElement().normalize();
                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xpath = xpf.newXPath();
                    result = xpath.evaluate(resultPath, doc);
                    break;
                default:
                    LOGGER.error("Solr search failed to respond as expected for url: " + queryUrl);
            }
            get.releaseConnection();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return result;
    }
    
    public static String getSolrResponseCount(String searchUrlBase, String qString, String filter) {
        long count = 0;
        CommonsHttpSolrServer solr;
        try {
            solr = getSolr(searchUrlBase);
            SolrQuery query = new SolrQuery();
            query = query.setQuery(qString);
            if (filter != null) query = query.setFilterQueries(filter);
            QueryResponse response;
            response = solr.query(query);
            count = response.getResults().getNumFound();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return Long.toString(count);
    }
    
    private static CommonsHttpSolrServer getSolr(String solrSearchUrlBase) throws MalformedURLException, SolrServerException {
        CommonsHttpSolrServer solr;
        solr = new CommonsHttpSolrServer(solrSearchUrlBase);
        solr.setBaseURL(solrSearchUrlBase);
        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        solr.query(solrQuery);
        return solr;
    }
}
