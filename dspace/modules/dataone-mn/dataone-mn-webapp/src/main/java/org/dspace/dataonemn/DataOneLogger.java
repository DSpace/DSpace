package org.dspace.dataonemn;


//import java.util.Date;
import java.io.IOException;

import org.joda.time.format.DateTimeFormat;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.ConfigurationManager;

import javax.servlet.http.HttpServletResponse;



/**
 * This class maintains the database of DataOne loggable events - logging requests as they occur and
 * generating log records when requested.
 *
 * @author Peter E. Midford
 **/
public class DataOneLogger {


    final String mySolr;
    final String myLogData;
    private static final CommonsHttpSolrServer solr;
    private static final boolean useProxies;

    static Logger log = Logger.getLogger(DataOneLogger.class.getName());

    static
    {
    //            log.info("solr.log.server:" + ConfigurationManager.getProperty("solr.log.server"));

        CommonsHttpSolrServer server = null;

    //    if (ConfigurationManager.getProperty("solr.log.server") != null)
    //    {
    //        try
    //        {
    //            server = new CommonsHttpSolrServer(ConfigurationManager.getProperty("solr.log.server"));
//                SolrQuery solrQuery = new SolrQuery()
//                        .setQuery("type:2 AND id:1");
//                server.query(solrQuery);
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//            }
//        }
        solr = server;
        

//        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
//            useProxies = true;
//        else
            useProxies = false;
        log.info("useProxies=" + useProxies);


    }
    
       
    
    public DataOneLogger(String solrServ, String logData) {
	mySolr = solrServ;
	myLogData = logData;

    }


    /**
     * @throws IOException 
     * @throws SolrServerException 
     **/
    public void log(LogEntry log) throws Exception{
        SolrInputDocument d = log.getSolrInputDocument();
        solr.add(d);
    }


    /**
     * @param log holds query fields to avoid passing a bunch of parameters, all of same type (String)
     */
    public LogResults getLogRecords(String sessionid, String fromDate, String toDate, String event, String pid, int start, int end){
        StringBuffer solrQuery = new StringBuffer(200);
        LogResults testResults = new LogResults(HttpServletResponse.SC_NOT_IMPLEMENTED,0,"");
	return testResults;
    }

    /**
     **/
    public static class LogResults{
        final int htmlErrorCode;
        final int htmlDetailCode;
	final String logRecords;

	protected LogResults(int err, int detail, String xmlText){
	    htmlErrorCode = err;
            htmlDetailCode = detail;
            logRecords = xmlText;
	}
    }

}