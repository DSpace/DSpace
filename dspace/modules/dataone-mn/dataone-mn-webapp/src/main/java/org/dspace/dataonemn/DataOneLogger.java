package org.dspace.dataonemn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.ConfigurationManager;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;



/**
 * This class maintains the database of DataOne loggable events - logging requests as they occur and
 * generating log records when requested.
 *
 * @author Peter E. Midford
 **/
public class DataOneLogger {

    public final static int DEFAULT_START = 0;
    public final static int DEFAULT_COUNT = 1000;
    
    /*
     * Per dataOne spec: http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Event
     */
    public final static String EVENT_CREATE = "create";
    public final static String EVENT_READ = "read";
    public final static String EVENT_UPDATE = "update";
    public final static String EVENT_DELETE = "delete";
    public final static String EVENT_REPLICATE = "replicate";
    public final static String EVENT_SYNCHRONIZATION_FAILED = "synchronization_failed";
    public final static String EVENT_REPLICATION_FAILED = "replication_failed";


    private final CommonsHttpSolrServer solr;

    private static final String SOLR_LOG_SERVER_PROPERTY = "solr.log.dataonemn.server";

    static Logger log = Logger.getLogger(DataOneLogger.class.getName());


    public DataOneLogger() {

        final String serverString = ConfigurationManager.getProperty(SOLR_LOG_SERVER_PROPERTY);
        log.info("solr.log.dataonemn.server from configuration :" + serverString);

        CommonsHttpSolrServer server = null;

        try{
            server = new CommonsHttpSolrServer(serverString);
            log.info("Solr server for dataone logging initialized from " + serverString);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            server = null;
        }
        finally{
            solr = server;
        }
 
    }


    /**
     * @throws IOException 
     * @throws SolrServerException 
     **/
    public void log(LogEntry le) throws Exception{
        long lastEntry = getNextEntry(solr);
        log.info("Value returned by nextEntry is " + lastEntry);
        SolrInputDocument d = le.getSolrInputDocument(lastEntry);
        if (solr != null){
            try {
                solr.add(d);
                // force an immediate update
                UpdateRequest req = new UpdateRequest();
                req.setAction( UpdateRequest.ACTION.COMMIT, false, false );
                req.add( d);
                UpdateResponse rsp = req.process(solr);
            } catch (SolrServerException e) {
                log.error("Solr exception when committing a log record",e);
            } catch (IOException e) {
                log.error("IO Exception when committing a log record", e);
            }
        }
        else {
            log.error("Solr server was not initialized from " + ConfigurationManager.getProperty(SOLR_LOG_SERVER_PROPERTY));
        }
    }
    
    private long getNextEntry(CommonsHttpSolrServer solr){
        final SolrQuery q = buildNextEntryQuery();
        try {
            QueryResponse rsp = solr.query(q);
            SolrDocumentList docs = rsp.getResults();
            if (docs.size()>0){
                SolrDocument d = docs.get(0);
                if (d.keySet().contains("entryId")){
                    Object raw = d.get("entryId");
                    return ((Long)raw).longValue()+1;
                }
                else{
                    log.info("Retrieving entryId failed; returning 0");
                    return (long) 0;    
                }
            }
            else{
                log.info("Response returned no results; was the index cleared?");
                return (long)0;
            }
        }
        catch (SolrServerException e) {
            log.error("Retrieving entryId caused a solr exception; returning 0; exception was: ",e);
            return (long) 0;
        }
        
    }
    
    private SolrQuery buildNextEntryQuery(){
        SolrQuery result = new SolrQuery("nodeIdentifier:urn*");
        result.addSortField("entryId", SolrQuery.ORDER.desc);
        return result;
    }

    final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n";
    final String logFooter = "</d1:log>";
    final String emptyResults = 
        xmlHeader +
        "<d1:log xmlns:d1=\"http://ns.dataone.org/service/types/v1\" count=\"0\" start=\"0\" total=\"0\"> \n" +
        logFooter;

    final String logHeaderTemplate = "<d1:log xmlns:d1=\"http://ns.dataone.org/service/types/v1\" count=\"%s\" start=\"%s\" total=\"%s\"> \n";
    
    public String buildLogHeader(int count, int start, long total){
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format(logHeaderTemplate,count,start,total);
        return sb.toString();
    }
    
    /**
     * Ideally use the LogEntry class to avoid all these strings 
     */
    public LogResults getLogRecords(Date fromDate,Date toDate, String event, String pidFilter, int start, int count ){
        final List<LogEntry> matchingEntries = new ArrayList<LogEntry>(); 
        final SolrQuery solrQuery = buildSolrQuery(fromDate,toDate,event,pidFilter);
        // Use setRows to specify count, otherwise rows is default from 
        // solrconfig.xml (typically 10).  In earlier version, rows was not
        // set to count and paging was performed on the results, which never
        // exceeded 10 items.
        solrQuery.setStart(start);
        solrQuery.setRows(count);
        long total = 0;
        try {
            QueryResponse rsp = solr.query(solrQuery);
            SolrDocumentList docs = rsp.getResults();
            total = docs.getNumFound(); // Total number of matches
            for(SolrDocument doc : docs){
                matchingEntries.add(new LogEntry(doc));
            }
            
        } catch (SolrServerException e) {
            log.error("Solr server threw an exception while retreiving log records: ",e);
            log.info("Solr query was: " + solrQuery.getQuery());
        }
        if (matchingEntries.size() == 0){
            // is this necessary, or correct?
           return new LogResults(HttpServletResponse.SC_OK,0,emptyResults);
        }
        final StringBuffer entryResults = new StringBuffer(200);
        for(LogEntry le : matchingEntries){
            entryResults.append(le.getXml());
        }
        StringBuffer xmlResults = new StringBuffer();
        xmlResults.append(xmlHeader);
        xmlResults.append(buildLogHeader(matchingEntries.size(),start,total));
        xmlResults.append(entryResults.toString());
        xmlResults.append(logFooter);
        LogResults results = new LogResults(HttpServletResponse.SC_OK,0,xmlResults.toString());
        return results;
    }


    private SolrQuery buildSolrQuery(Date fromDate,Date toDate, String event, String pidFilter){
        final SolrQuery result = new SolrQuery("nodeIdentifier:urn*");
        result.addSortField("dateLogged", SolrQuery.ORDER.asc);
        if (event != null){
            result.addFilterQuery("event:" + event);
            log.info("Adding event Filter: " + event);
        }
        if (fromDate != null){
            final String zFromDate = convertDate(fromDate)+"Z";
            if (toDate != null){
                final String zToDate = convertDate(toDate)+"Z";
                result.addFilterQuery("dateLogged:[" + zFromDate + " TO " + zToDate + "]");
            }
            else {
                result.addFilterQuery("dateLogged:[" + zFromDate + " TO NOW]");                
            }
        }
        else{
            if (toDate != null){
                final String zToDate = convertDate(toDate)+"Z";
                result.addFilterQuery("dateLogged:[* TO " + zToDate + "]");
            }
        }
        if (pidFilter != null && !pidFilter.equalsIgnoreCase("null")){
            //need to escape the colon in the dryad identifier so solr will accept it
            int colonpos = pidFilter.indexOf(':');
            if (colonpos>-1){  //expect it to be there, but to be safe
                pidFilter = pidFilter.substring(0,colonpos) + "\\:" + pidFilter.substring(colonpos+1);
            }
            result.addFilterQuery("identifier:" + pidFilter);
            log.info("Adding pid filter: " + pidFilter);
        }
        return result;
    }

    
    static final DateTimeFormatter tFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    protected static String convertDate(Date d){
        final DateTime jDate = new DateTime(d); //TODO format this correctly
        final DateTime zDate = jDate.withZone(DateTimeZone.UTC);
        String dString = zDate.toString(tFormat);
        dString = dString.substring(0,dString.indexOf('+'));
        return dString;
    }

    /**
     * Generate the xml and return bundle of error code, detail Code (?, but 
     * it's in the dataone spec), and the xml string
     **/
    public static class LogResults{
        private final int htmlErrorCode;
        private final int htmlDetailCode;
        private final String logRecords;

        private LogResults(int err, int detail, String xmlText){
            htmlErrorCode = err;
            htmlDetailCode = detail;
            logRecords = xmlText;
        }

        public String getLogRecords(){
            return logRecords;
        }

        public int htmlDetailCode(){
            return htmlDetailCode;
        }

        public int htmlErrorCode(){
            return htmlErrorCode;
        }
    }

}
