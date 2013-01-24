/*
 * 
 * Created on Jun 3, 2012
 * Last updated on Jun 11, 2012
 * 
 */
package org.dspace.dataonemn;

import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * corresponds to the type defined here:
 *  http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html?highlight=types.log#Types.LogEntry
 */
public class LogEntry{
    
    final static String IDENTIFIERKEY = "identifier";
    final static String IPADDRESSKEY = "ipAddress";
    final static String USERAGENTKEY = "userAgent";
    final static String SUBJECTKEY = "subject";
    final static String EVENTKEY = "event";
    final static String DATELOGGEDKEY = "dateLogged";
    final static String NODEIDENTIFIERKEY = "nodeIdentifier";
    final static String ENTRYIDKEY = "entryId";
    
    String identifier;
    String ipAddress;
    String userAgent;
    String subject;
    String event;
    String dateLogged;
    long entryId;
    String nodeIdentifier;
    
    static Logger log = Logger.getLogger(LogEntry.class.getName());

    public LogEntry(){
        
    }
    
    //This doesn't seem to be picking up values...
    public LogEntry(SolrDocument doc) {
        if (doc.keySet().contains(IDENTIFIERKEY)){
            identifier = doc.get(IDENTIFIERKEY).toString();
        }
        if (doc.keySet().contains(IPADDRESSKEY)){
            ipAddress = doc.get(IPADDRESSKEY).toString();
        }
        if (doc.keySet().contains(USERAGENTKEY)){
            userAgent = doc.get(USERAGENTKEY).toString();
        }
        if (doc.keySet().contains(SUBJECTKEY)){
            subject = doc.get(SUBJECTKEY).toString();
        }
        if (doc.keySet().contains(EVENTKEY)){
            event = doc.get(EVENTKEY).toString();
        }
        if (doc.keySet().contains(DATELOGGEDKEY)){
            dateLogged = DataOneLogger.convertDate((Date)doc.get(DATELOGGEDKEY));
        }
        if (doc.keySet().contains(ENTRYIDKEY)){
            entryId = ((Long)doc.get(ENTRYIDKEY)).longValue();
        }
        if (doc.keySet().contains(NODEIDENTIFIERKEY)){
            nodeIdentifier = doc.get(NODEIDENTIFIERKEY).toString();
        }
    }

    public void setIdentifier(String idStr){
        identifier= idStr;
    }
    
    public void setIPAddress(String ipAddressStr){
        ipAddress = ipAddressStr;
    }
    
    public void setUserAgent(String userAgentStr){
        userAgent = userAgentStr;
    }
    
    public void setSubject(String subjectStr){
        subject = subjectStr;
    }
    
    public void setEvent(String eventStr){
        event = eventStr;
    }
    
    public void setNodeIdentifier(String nodeIdStr){
        nodeIdentifier = nodeIdStr;
    }
    
    public SolrInputDocument getSolrInputDocument(long index) {
        SolrInputDocument d = new SolrInputDocument();
        DateTime now = new DateTime(DateTimeZone.UTC); //TODO format this correctly
        DateTimeFormatter f = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSZZ");
        String nowString = now.toString(f);
        nowString = nowString.substring(0,nowString.indexOf('+'))+'Z';
        log.info("Now string is: " + nowString);
        d.addField(IDENTIFIERKEY, identifier);
        d.addField(DATELOGGEDKEY,nowString);
        d.addField(EVENTKEY, event);
        d.addField(IPADDRESSKEY,ipAddress);
        d.addField(NODEIDENTIFIERKEY,nodeIdentifier);
        d.addField(USERAGENTKEY, userAgent);
        d.addField(ENTRYIDKEY, index);
        d.addField(SUBJECTKEY,subject);

        return d;
    }
    
    public String getXml(){
        StringBuilder sb = new StringBuilder(500);
        sb.append("<logEntry>");
        sb.append("<entryId>");
        sb.append(entryId);
        sb.append("</entryId>");
        sb.append("<identifier>");
        sb.append(identifier);
        sb.append("</identifier>");
        sb.append("<ipAddress>");
        sb.append(ipAddress);
        sb.append("</ipAddress>");
        sb.append("<userAgent>");
        sb.append(userAgent);
        sb.append("</userAgent>");
        sb.append("<subject>");
        sb.append(subject);
        sb.append("</subject>");
        sb.append("<event>");
        sb.append(event);
        sb.append("</event>");
        sb.append("<dateLogged>");
        sb.append(dateLogged);
        sb.append("</dateLogged>");
        sb.append("<nodeIdentifier>");
        sb.append(nodeIdentifier);
        sb.append("</nodeIdentifier>");
        sb.append("</logEntry>");
        return sb.toString();
    }
}