package org.datadryad.dspace.statistics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SiteOverviewStats  {

    private static final Logger LOGGER = Logger.getLogger(SiteOverviewStats.class);

    private static final String FILTER_30_DAY = "&fq=dc.date.issued_dt%3A%5BNOW-90DAY%20TO%20NOW%5D";
    
    private static final String PUB_SEARCH = "/select/?q=DSpaceStatus:Archived&facet=on&rows=0&facet.field=prism.publicationName_filter&fq=location:l2&facet.limit=-1";
    private static final String PUB_SEARCH_30DAY = PUB_SEARCH + FILTER_30_DAY;

    private static final String AUTH_SEARCH = "/select/?q=DSpaceStatus:Archived&facet=on&rows=0&facet.field=dc.contributor.author_filter&fq=location:l2&facet.limit=-1";
    private static final String AUTH_SEARCH_30DAY = AUTH_SEARCH + FILTER_30_DAY;

    private static final String DOWN_SEARCH_30DAY = "/select/?q=*%3A*&fq=type%3A0&fq=time:%5BNOW-30DAY%20TO%20NOW%5D";
    private static final String DOWN_SEARCH = "/select/?q=*%3A*&fq=type%3A0";

    private static final String PUB_COUNTER = "count(//lst[@name='prism.publicationName_filter']/int[.!='0'])";
    private static final String AUTH_COUNTER = "count(//lst[@name='dc.contributor.author_filter']/int[.!='0'])";
    private static final String DOWN_COUNTER = "//result/@numFound";

    private static final String solrSearchUrlBase = ConfigurationManager.getProperty("solr.search.server");
    private static final String solrStatsUrlBase = ConfigurationManager.getProperty("solr.stats.server");
    
    // formatted date string for -30 days (last 30 days)
    private static String formattedThirtyDayPrior;
    static {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        formattedThirtyDayPrior = dateFormat.format(cal.getTime());
    }

    private Context context;
    private CommonsHttpSolrServer solr;
    
    private String dataFileCount;
    private String dataFileCount_30day;
    private String dataPackageCount;
    private String dataPackageCount_30day;
    private String journalCount;
    private String journalCount_30day;
    private String uniqAuthors;
    private String uniqAuthors_30day;
    private String totalFileDownload;
    private String totalFileDownload_30day;
    
    public String getDataFileCount() {
        return this.dataFileCount;
    }
    public String getDataFileCount_30day() {
        return this.dataFileCount_30day;
    }
    public String getDataPackageCount() {
        return this.dataPackageCount;
    }
    public String getDataPackageCount_30day() {
        return this.dataPackageCount_30day;
    }
    public String getJournalCount() {
        return this.journalCount;
    }
    public String getJournalCount_30day() {
        return this.journalCount_30day;
    }
    public String getUniqAuthors() {
        return this.uniqAuthors;
    }
    public String getUniqAuthors_30day() {
        return this.uniqAuthors_30day;
    }
    public String getTotalFileDownload() {
        return this.totalFileDownload;
    }
    public String getTotalFileDownload_30day() {
        return this.totalFileDownload_30day;
    }
    
    public SiteOverviewStats(Context context) throws SQLException, StatisticsException, MalformedURLException, SolrServerException {
        this.context = context;
        solr = getSolr();
        getStats();
    }

    private void getStats() {
        // TODO: rewrite these to use xpaths and remove getCollectionCount(), getSolrResponseCount
        this.dataFileCount = getCollectionCount("stats.datafiles.coll", null);
        this.dataFileCount_30day = getCollectionCount("stats.datafiles.coll", formattedThirtyDayPrior);
        this.dataPackageCount = getSolrResponseCount("location:l2 AND DSpaceStatus:Archived", null);
        this.dataPackageCount_30day = getSolrResponseCount("location:l2 AND DSpaceStatus:Archived", "dc.date.issued_dt:[NOW-30DAY TO NOW]");
        // /TODO
        this.journalCount = getSolrXPathResult(solrSearchUrlBase + PUB_SEARCH, PUB_COUNTER);
        this.journalCount_30day = getSolrXPathResult(solrSearchUrlBase + PUB_SEARCH_30DAY, PUB_COUNTER);
        this.uniqAuthors = getSolrXPathResult(solrSearchUrlBase + AUTH_SEARCH, AUTH_COUNTER);
        this.uniqAuthors_30day = getSolrXPathResult(solrSearchUrlBase + AUTH_SEARCH_30DAY, AUTH_COUNTER);
        this.totalFileDownload = getSolrXPathResult(solrStatsUrlBase + DOWN_SEARCH, DOWN_COUNTER);
        this.totalFileDownload_30day = getSolrXPathResult(solrStatsUrlBase + DOWN_SEARCH_30DAY, DOWN_COUNTER);
    }
    
    private String getCollectionCount(String colName, String formattedDate) {
        Integer result = 0;
        try {
            Collection col = (Collection) HandleManager.resolveToObject(
                this.context, 
                ConfigurationManager.getProperty(colName));
            result = formattedDate != null
                    ? col.countItems(formattedDate)
                    : col.countItems();
        } catch (ClassCastException ex) {
            LOGGER.error("stats.datafiles.coll property isn't set properly");
        } catch (IllegalStateException ex) {
            LOGGER.error("stats.datafiles.coll property isn't set properly");
        } catch (SQLException ex) {
            LOGGER.error("stats.datafiles.coll property isn't set properly");
        }
        return Integer.toString(result);
    }
    
    private String getSolrResponseCount(String qString, String filter) {
        long count = 0;
        try {
            SolrQuery query = new SolrQuery();
            query = query.setQuery(qString);
            if (filter != null) query = query.setFilterQueries(filter);
            QueryResponse response = solr.query(query);
            count = response.getResults().getNumFound();
        } catch (SolrServerException details) {
            LOGGER.error(details.getMessage(), details);
        }
        return Long.toString(count);
    }

    private String getSolrXPathResult(String queryUrl, String resultPath) {
        String result = "";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            GetMethod get = new GetMethod(queryUrl);

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
        }
        catch (ParserConfigurationException details) {
            LOGGER.error(details.getMessage(), details);
        }
        catch (XPathExpressionException details) {
            LOGGER.error(details.getMessage(), details);
        } catch (SAXException ex) {
            java.util.logging.Logger.getLogger(SiteOverviewStats.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SiteOverviewStats.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    private CommonsHttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        CommonsHttpSolrServer solr;
        solr = new CommonsHttpSolrServer(solrSearchUrlBase);
        solr.setBaseURL(solrSearchUrlBase);
        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        solr.query(solrQuery);
        return solr;
    }

}
