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
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.search.SolrIndexSearcher;

public class CrisMetricsUpdateListener implements SolrEventListener
{
    private static Map<String, Date> cacheAcquisition = new HashMap<String, Date>();
    
    private static Map<String, Long> cacheVersion = new HashMap<String, Long>();
    
    private static final int cacheValidity = 24*60*60000;

    private static final Map<String, PopulateRanksThread> underRebuild = new HashMap<String, PopulateRanksThread>();
    
    private static final Map<String, Map<String, Map<Integer, Double>>> metrics = new HashMap<String, Map<String, Map<Integer, Double>>>();

    private static final Map<String, Map<String, Map<Integer, ExtraInfo>>> extraInfo = new HashMap<String, Map<String, Map<Integer, ExtraInfo>>>();
    
    private SolrCore core;

    public CrisMetricsUpdateListener(SolrCore core)
    {
        this.core = core;
    }

    ////////////// SolrEventListener methods /////////////////

    @Override
    public void init(NamedList args)
    {
    	/* NOOP */
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

    public static Double getMetric(String coreName, String metric, int docId)
    {
    	final Thread underRebuildThread = underRebuild.get(coreName);
		if (underRebuildThread != null) {
    		return null;
    	}
    	Map<String, Map<Integer, Double>> m = metrics.get(coreName);
        if (m != null && m.containsKey(metric))
        {
            Map<Integer, Double> values = m.get(metric);
            if (values.containsKey(docId))
            {
                return values.get(docId);
            }
        }
        return null;
    }

    public static ExtraInfo getRemark(String coreName, String metric, int docId)
    {
    	final Thread underRebuildThread = underRebuild.get(coreName);
		if (underRebuildThread != null) {
    		return null;
    	}
    	Map<String, Map<Integer, ExtraInfo>> ei = extraInfo.get(coreName);
        if (ei != null && ei.containsKey(metric))
        {
            Map<Integer, ExtraInfo> values = ei.get(metric);
            if (values.containsKey(docId))
            {
                return values.get(docId);
            }
        }
        return null;
    }
    
    private static Map<String, String> getDBProps(SolrCore core) {
		Map<String, String> dbprops = new HashMap<String, String>();

		try {
			SolrResourceLoader loader = core.getResourceLoader();
			List<String> lines = loader.getLines("database.properties");
			for (String line : lines) {
				if (StringUtils.isEmpty(line) || line.startsWith("#")) {
					continue;
				}
				String[] kv = StringUtils.split(line, "=");
				dbprops.put(kv[0], kv[1]);
			}
			Class.forName(dbprops.get("database.driverClassName"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dbprops;
    }

	public static void renewCache(SolrIndexSearcher newSearcher) throws IOException
    {
		String coreName = newSearcher.getCore().getName();
		PopulateRanksThread underRebuildThread = null;
		synchronized (CrisMetricsUpdateListener.class) {
			underRebuildThread = underRebuild.get(coreName);
			if (underRebuildThread != null) {
				log.debug("rank chache already under rebuild... restart");
				underRebuildThread.stopGraceful();
			}
		}
		underRebuildThread = new PopulateRanksThread(newSearcher);
		underRebuildThread.start();
    	underRebuild.put(coreName, underRebuildThread);
    }

    public static Map<String, Map<Integer, Double>> getMetrics(String coreName)
    {
        return metrics.get(coreName);
    }

    public static Map<String, Map<Integer, ExtraInfo>> getExtrainfo(String coreName)
    {
        return extraInfo.get(coreName);
    }
    
	public static boolean isCacheInvalid(SolrIndexSearcher searcher) {
		String coreName = searcher.getCore().getName();
		Date ca = cacheAcquisition.get(coreName);
		Long cv = cacheVersion.get(coreName);
		Date now = new Date();
		return ca == null || (now.getTime() - ca.getTime() > cacheValidity)
				|| cv.longValue() != searcher.getOpenTime();
	}

	public static class PopulateRanksThread extends Thread {
		private boolean stop = false;
		
		private SolrIndexSearcher newSearcher;
		
		public PopulateRanksThread(SolrIndexSearcher newSearcher) {
			this.newSearcher = newSearcher;
		}
		
		public void stopGraceful() {
			this.stop = true;
		}

		@Override
		public void run() {
			String coreName = newSearcher.getCore().getName();
			try {
				log.debug("Building the rank chache...");
	    		cacheVersion.put(coreName, newSearcher.getOpenTime());
	            cacheAcquisition.put(coreName, new Date());
				populateRanks(coreName, newSearcher);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
				underRebuild.put(coreName, null);
			}
		}

		private void populateRanks(String coreName, SolrIndexSearcher searcher)
		        throws IOException
		{	
			Integer numDocs = (Integer) searcher.getStatistics().get("numDocs");
			Date start = new Date();
		    Map<String, Map<Integer, Double>> metricsCopy = new HashMap<String, Map<Integer, Double>>(numDocs);
		    Map<String, Map<Integer, ExtraInfo>> metricsRemarksCopy = new HashMap<String, Map<Integer, ExtraInfo>>(numDocs);
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
		        	if (stop) {
		        		return;
		        	}
		            // find Lucene docId for uid
		        	searchIDCache.put(searcher.doc(doc.doc, fields).getValues("search.uniqueid")[0], doc.doc);
		        }
		        Date endSearch = new Date();            
		        long searcherTime = endSearch.getTime() - startSearch.getTime();
		        Map<String, String> dbprops = getDBProps(searcher.getCore());
		        
		        Date startQuery = new Date();
		        conn = DriverManager.getConnection(dbprops.get("database.url"),
		                dbprops.get("database.username"),
		                dbprops.get("database.password"));
		        ps = conn.prepareStatement(
		                "select resourceid, resourcetypeid, metrictype, remark, metriccount, timestampcreated, startdate, enddate from cris_metrics where last = true");
		        rs = ps.executeQuery();
		        log.debug("QUERY TIME:" + (new Date().getTime()-startQuery.getTime()));
		        
		        while (rs.next())
		        {
		        	if (stop) {
		        		return;
		        	}
		            Integer resourceId = (Integer)rs.getObject(1);
		            int resourceTypeId = rs.getInt(2);
		            double count = rs.getDouble(5);
		            String type = rs.getString(3);
		            String remark = rs.getString(4);
		            Date acqTime = rs.getDate(6);
		            Date startTime = rs.getDate(7);
		            Date endTime = rs.getDate(8);
		            Integer docId = searchIDCache.get(resourceTypeId+"-"+resourceId);
		            if (docId != null) {
		                String key = new StringBuffer("crismetrics_").append(type.toLowerCase()).toString();
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
		                tmpSubRemarkMap.put(docId, new ExtraInfo(remark, acqTime, startTime, endTime));
		
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
		    
		    Map<String, Map<Integer, Double>> m = metrics.get(coreName); 
		    Map<String, Map<Integer, ExtraInfo>> ei = extraInfo.get(coreName);
		    
		    if (ei != null) {
		    	ei.clear();
		    }
		
		    ei = metricsRemarksCopy;
		    extraInfo.put(coreName, ei);
		
		    if (m != null) {
		    	m.clear();
		    }
		    
		    m = metricsCopy;
		    metrics.put(coreName, m);
		}

	}
}