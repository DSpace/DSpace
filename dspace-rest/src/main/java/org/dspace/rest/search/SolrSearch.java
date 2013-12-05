/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.search;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;

import org.apache.log4j.Logger;

public class SolrSearch implements Search{
	
	private static final Logger log = Logger.getLogger(SolrSearch.class);
	
	@Override
	public ArrayList<org.dspace.rest.common.Item> search(Context context, HashMap<String,String>searchTerms, String expand, Integer limit, Integer offset, String sortField, String sortOrder){
		ArrayList<org.dspace.rest.common.Item> results = new ArrayList<org.dspace.rest.common.Item>();
		StringBuffer query_string=new StringBuffer();
		Iterator<String> keyset= searchTerms.keySet().iterator();
		while(keyset.hasNext()){
			String key = keyset.next();
			query_string.append(key+":"+searchTerms.get(key));
			if(keyset.hasNext()){
				query_string.append(" AND ");
			}
		}
		log.debug("serach query: " + query_string.toString());
		SolrServiceImpl solr = new SolrServiceImpl();
		DiscoverQuery query = new DiscoverQuery();
		query.setQuery(query_string.toString());
		if(sortField!=null && sortOrder!=null){
			query.setSortField(sortField, sortOrder.compareTo("asc")==0?DiscoverQuery.SORT_ORDER.asc:DiscoverQuery.SORT_ORDER.desc);
		}
		int added =0;
		int current=0;
		try {
			DiscoverResult result =solr.search(context, query);
			List<DSpaceObject>  list=result.getDspaceObjects();
			for(DSpaceObject obj : list){
				if(obj instanceof org.dspace.content.Item && current>=offset){
					results.add(new org.dspace.rest.common.Item((org.dspace.content.Item)obj, expand, context));
					added++;
				}
				current++;
				if(added>=limit){
					break;
				}
			}
		} catch (SearchServiceException e) {
			log.error(e.toString());
		} catch (SQLException e) {
			log.error(e.toString());
		}
		
		return results;
	}

}
