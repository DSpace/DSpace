/*
 * EthOntos - a tool for comparative methods using ontologies
 * Copyright 2004-2005 Peter E. Midford
 * 
 * Created on Jun 3, 2012
 * Last updated on Jun 3, 2012
 * 
 */
package org.dspace.dataonemn;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;

/**
 * corresponds to the type defined here:
 *  http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html?highlight=types.log#Types.LogEntry
 */
public class LogEntry{
    final private Map<String,String> fields = new HashMap<String,String>();
    
    
    static Logger log = Logger.getLogger(LogEntry.class.getName());

    
    public void setEntryId(String id){
        fields.put("entryId", id);
    }
    
    public void setIdentifier(String id){
        fields.put("identifier", id);
    }

    public void setIPAddress(String ipAddress){
        fields.put("ipAddress", ipAddress);
    }
    
    public void setUserAgent(String userAgent){
        fields.put("userAgent",userAgent);
    }
    
    public void setSubject(String sub){
        fields.put("subject", sub);
    }
    
    public void setEvent(String event){
        fields.put("event", event);
    }
    
    public void setDateLogged(String date){
        fields.put("dateLogged", date);
    }
    
    public void setNodeIdentifier(String id){
        fields.put("NodeIdentifier", id);
    }
    
    public SolrInputDocument getSolrInputDocument() {
        setEntryId(Long.toString(System.nanoTime()));   //need to generate this correctly
        SolrInputDocument d = new SolrInputDocument();
        for(String fieldName : fields.keySet())
            d.addField(fieldName,fields.get(fieldName));
        return d;
    }
    
    
}