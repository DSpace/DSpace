/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.pmc.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.core.ConfigurationManager;

public class PMCEntrezLocalSOLRServices
{
    private static final HttpSolrServer solr;
    
    private static Logger log = Logger.getLogger(PMCEntrezLocalSOLRServices.class);
    
    static
    {
        
        String property = ConfigurationManager.getProperty("cris", "pmc.server");
        log.info("solr.pmc.server:" + property);
        
        HttpSolrServer server = null;
        
        if (property != null)
        {
            try
            {                
                server = new HttpSolrServer(property);
                SolrQuery solrQuery = new SolrQuery()
                        .setQuery("DOI:a AND PMCID:1 AND PMID:1");
                server.query(solrQuery);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        solr = server;        
       
    }
    
    public List<Integer> getPubmedIDs(Integer pmcID) throws PMCEntrezException
    {
        return getMultiPubmedIDs(pmcID).get(pmcID);
    }

    public Map<Integer, List<Integer>> getMultiPubmedIDs(Integer... pmcID)
            throws PMCEntrezException
    {
        String[] spmcID = new String[pmcID.length];
        for (int i  = 0; i < pmcID.length; i++)
        {
            spmcID[i] = "PMC" + pmcID[i];
        }
        Map<String, List<Integer>> tmp = getMultiIDs("PMID", "PMCID",
                String.class, spmcID);
        
        Map<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
        for (int i  = 0; i < pmcID.length; i++)
        {
            result.put(pmcID[i], tmp.get(spmcID[i]));
        }
        return result;
    }


    public List<Integer> getPMCIDs(Integer pubmedID) throws PMCEntrezException
    {
        return getMultiPMCIDs(pubmedID).get(pubmedID);
    }

    public Map<Integer, List<Integer>> getMultiPMCIDs(Integer... pubmedID)
            throws PMCEntrezException
    {
        return getMultiIDs("PMCID", "PMID", Integer.class, pubmedID);
    }

    public List<Integer> getPubmedIDs(String doi) throws PMCEntrezException
    {
        return getMultiIDs("PMID", "DOI", String.class, doi).get(doi);
    }

    private <T, K> Map<K, List<Integer>> getMultiIDs(String returnID,
            String lookupField, Class<K> clazz, T... lookupValues)
            throws PMCEntrezException
    {
        SolrQuery query = new SolrQuery();
        query.setQuery(lookupField
                + ":\""
                + org.springframework.util.StringUtils.arrayToDelimitedString(
                        lookupValues, "\" OR \"") + "\"");
        query.setRows(Integer.MAX_VALUE);
        query.setFields(returnID, lookupField);
        QueryResponse qresp = null;
        try
        {
            qresp = solr.query(query);
        }
        catch (SolrServerException e)
        {
            throw new PMCEntrezException("Failed to query the PMC Solr Server",
                    e);
        }
        Map<K, List<Integer>> result = new HashMap<K, List<Integer>>();
        for (SolrDocument doc : qresp.getResults())
        {
            List<Integer> tmp = result.get(doc.getFieldValue(lookupField));
            if (tmp == null)
            {
                tmp = new ArrayList<Integer>();
                K key = (K) doc.getFieldValue(lookupField);
                result.put(key, tmp);
            }
            if (doc.getFieldValue(returnID) != null)
            {
                if ("PMCID".equals(returnID))
                {
                    tmp.add(Integer.valueOf(((String) doc
                            .getFieldValue(returnID)).substring(3)));
                }
                else
                {
                    tmp.add((Integer) doc.getFieldValue(returnID));
                }
            }
        }
        return result;
    }
    
    public static void main(String[] args) throws PMCEntrezException
    {
        PMCEntrezLocalSOLRServices serv = new PMCEntrezLocalSOLRServices();
        System.out.println(serv.getPubmedIDs(50272));// 1279667
        System.out.println(serv.getPMCIDs(1279667));// 50272
        System.out.println(serv.getPubmedIDs("10.1038/onc.2010.235"));// 20562908
    }
}
