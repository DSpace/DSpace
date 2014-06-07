/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;


import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.statistics.SolrLogger;

/**
 * This class is mainly a copy and paste of the org.dspace.statistics.SolrLogger class used to manage logging 
 * of access to Researcher Page objects.
 * 
 * Static SolrLogger used to hold HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries.
 * 
 * @author pascarelli, bollini
 */
public class CrisSolrLogger extends SolrLogger
{
	
	private static Logger log = Logger.getLogger(CrisSolrLogger.class);
	
	
	@Override
	public SolrDocumentList getRawData(int type) throws SolrServerException
	{

	    if (type >= CrisConstants.CRIS_TYPE_ID_START)
        {
            
	        SolrQuery query = new SolrQuery();
	        query.setQuery("*:*");
	        query.setFilterQueries("type:" + type);
	        query.setRows(Integer.MAX_VALUE);
	        query.setFields("ip", "id", "sectionid", "type", "time", "dns", "epersonid",
	                "isBot", "userAgent");
	        QueryResponse resp = getSolr().query(query);
	        return resp.getResults();
	        
        }
        else
        {
            return super.getRawData(type);
        }

	}
	
    
}
