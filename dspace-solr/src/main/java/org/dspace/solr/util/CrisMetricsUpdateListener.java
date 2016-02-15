/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.search.SolrIndexSearcher;

public class CrisMetricsUpdateListener implements SolrEventListener
{
    private static Date cacheAcquisition;
    
    private static long cacheVersion;
    
    private static final int cacheValidity = 24*60*60000;

    private static final Map<String, Map<Integer, Double>> metrics = new HashMap<String, Map<Integer, Double>>();

    private static final Map<String, Map<Integer, ExtraInfo>> extraInfo = new HashMap<String, Map<Integer, ExtraInfo>>();
    
    private SolrCore core;

    private static Map<String, String> dbprops;

    public CrisMetricsUpdateListener(SolrCore core)
    {
        this.core = core;
        dbprops = new HashMap<String, String>();
    }

    ////////////// SolrEventListener methods /////////////////

    @Override
    public void init(NamedList args)
    {
        try
        {
            SolrResourceLoader loader = core.getResourceLoader();
            List<String> lines = loader.getLines("database.properties");
            for (String line : lines)
            {
                if (StringUtils.isEmpty(line) || line.startsWith("#"))
                {
                    continue;
                }
                String[] kv = StringUtils.split(line, "=");
                dbprops.put(kv[0], kv[1]);
            }
            Class.forName(dbprops.get("database.driverClassName"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void newSearcher(SolrIndexSearcher newSearcher,
            SolrIndexSearcher currentSearcher)
    {
    	/* NOOP */ 
    }

    @Override
    public void postCommit()
    {
        /* NOOP */ 
    }

    @Override
    public void postSoftCommit()
    {
        /* NOOP */ 
    }

    ////////////// Service methods /////////////////////

    public static Double getMetric(String metric, int docId)
    {

        if (metrics.containsKey(metric))
        {
            Map<Integer, Double> values = metrics.get(metric);
            if (values.containsKey(docId))
            {
                return values.get(docId);
            }
        }
        return null;
    }

    public static ExtraInfo getRemark(String metric, int docId)
    {

        if (extraInfo.containsKey(metric))
        {
            Map<Integer, ExtraInfo> values = extraInfo.get(metric);
            if (values.containsKey(docId))
            {
                return values.get(docId);
            }
        }
        return null;
    }
    
    private static synchronized void populateRanks(SolrIndexSearcher searcher)
            throws IOException
    {	
    	Date start = new Date();
        Map<String, Map<Integer, Double>> metricsCopy = new HashMap<String, Map<Integer, Double>>();
        Map<String, Map<Integer, ExtraInfo>> metricsRemarksCopy = new HashMap<String, Map<Integer, ExtraInfo>>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            ScoreDoc[] hits = searcher.search(
            		new MatchAllDocsQuery(),
            		Integer.MAX_VALUE
            		).scoreDocs;

            Map<String, Integer> searchIDCache = new HashMap<String, Integer>(hits.length);
            Set<String> fields = new HashSet<String>();
            fields.add("search.uniqueid");
            Date startSearch = new Date();
            for (ScoreDoc doc : hits) {
                // find Lucene docId for uid
            	searchIDCache.put(searcher.doc(doc.doc, fields).getValues("search.uniqueid")[0], doc.doc);
            }
            Date endSearch = new Date();            
            long searcherTime = endSearch.getTime() - startSearch.getTime();
            
            Date startQuery = new Date();
            conn = DriverManager.getConnection(dbprops.get("database.url"),
                    dbprops.get("database.username"),
                    dbprops.get("database.password"));
            ps = conn.prepareStatement(
                    "select resourceid, resourcetypeid, metrictype, remark, metriccount,timestampcreated from cris_metrics where last = true");
            rs = ps.executeQuery();
            log.debug("QUERY TIME:" + (new Date().getTime()-startQuery.getTime()));
            
            while (rs.next())
            {
                int resourceId = rs.getInt(1);
                int resourceTypeId = rs.getInt(2);
                double count = rs.getDouble(5);
                String type = rs.getString(3);
                String remark = rs.getString(4);
                Date acqTime = rs.getDate(6);
                Integer docId = searchIDCache.get(resourceTypeId+"-"+resourceId);
                if (docId != null) {
	                String key = "crismetrics_" + type.toLowerCase();
	                Map<Integer, Double> tmpSubMap;
	                Map<Integer, ExtraInfo> tmpSubRemarkMap;
	                boolean add = false;
	                if(metricsCopy.containsKey(key)) {
	                    tmpSubMap = metricsCopy.get(key);
	                    tmpSubRemarkMap = metricsRemarksCopy.get(key);
	                }
	                else {
	                	add = true;
	                	tmpSubMap = new HashMap<Integer, Double>();
		                tmpSubRemarkMap = new HashMap<Integer, ExtraInfo>();
	                }
                
                    tmpSubMap.put(docId, count);
                    tmpSubRemarkMap.put(docId, new ExtraInfo(remark, acqTime));
	
	                if(add) {
	                    metricsCopy.put(key, tmpSubMap);
	                    metricsRemarksCopy.put(key, tmpSubRemarkMap);
	                }
                }
            }
            Date end = new Date();
            log.debug("SEARCH TIME: "+searcherTime);
            log.debug("RENEW CACHE TIME: "+(end.getTime()-start.getTime()));
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            throw new IOException(e);
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    /* NOOP */
                	e.printStackTrace();
                }
            }
        }
        extraInfo.clear();
        metrics.clear();
        extraInfo.putAll(metricsRemarksCopy);
        metrics.putAll(metricsCopy);
    }

    public static void renewCache(SolrIndexSearcher newSearcher) throws IOException
    {
    	cacheVersion = newSearcher.getOpenTime();
        cacheAcquisition = new Date();
        populateRanks(newSearcher);
    }

    public static Map<String, Map<Integer, Double>> getMetrics()
    {
        return metrics;
    }

    public static Map<String, Map<Integer, ExtraInfo>> getExtrainfo()
    {
        return extraInfo;
    }
    
	public static boolean isCacheInvalid(SolrIndexSearcher searcher) {
      Date now = new Date();
		return cacheAcquisition == null || (now.getTime() - cacheAcquisition.getTime() > cacheValidity)
				|| cacheVersion != searcher.getOpenTime();
	}
}
