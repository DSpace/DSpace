/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.search;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.rest.common.Item;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SolrSearch implements Search{
	
	private static final Logger log = Logger.getLogger(SolrSearch.class);
	
	private DiscoverResult result;
	
	@Override
	public ArrayList<org.dspace.rest.common.Item> search(Context context, HashMap<String,String>searchTerms, String expand, Integer limit, Integer offset, String sortField, String sortOrder){
		ArrayList<org.dspace.rest.common.Item> results = new ArrayList<org.dspace.rest.common.Item>();
		StringBuilder query_string=new StringBuilder();
		Iterator<String> keyset= searchTerms.keySet().iterator();
		while(keyset.hasNext()){
			String key = keyset.next();
			query_string.append(key).append(":").append(searchTerms.get(key));
			if(keyset.hasNext()){
				query_string.append(" AND ");
			}
		}
		log.debug("search query: " + query_string.toString());
		SolrServiceImpl solr = new SolrServiceImpl();
		DiscoverQuery query = new DiscoverQuery();
		query.setQuery(query_string.toString());
		query.setMaxResults(limit);
		query.setStart(offset);
		if(sortField!=null && sortOrder!=null){
			query.setSortField(sortField, sortOrder.compareTo("asc")==0?DiscoverQuery.SORT_ORDER.asc:DiscoverQuery.SORT_ORDER.desc);
		}
		try {
			result =solr.search(context, query);
			List<DSpaceObject>  list=result.getDspaceObjects();
			for(DSpaceObject obj : list){
				if(obj instanceof org.dspace.content.Item && AuthorizeManager.authorizeActionBoolean(context, obj, org.dspace.core.Constants.READ)) {
					results.add(new org.dspace.rest.common.Item((org.dspace.content.Item)obj, expand, context));
				}
			}
		} catch (SearchServiceException e) {
			log.error(e.getMessage());
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		
		return results;
	}

	@Override
	public long getTotalCount() {
		if(result!=null){
			return result.getTotalSearchResults();
		}
		return 0;
	}

	@Override
	public ArrayList<Item> searchAll(Context context, String query,
			String expand, Integer limit, Integer offset, String sortField,
			String sortOrder) {
		ArrayList<org.dspace.rest.common.Item> results = new ArrayList<org.dspace.rest.common.Item>();
		StringBuilder query_string=new StringBuilder();
		query_string.append("{!lucene q.op=AND}");
		query_string.append(query);
		log.debug("search query: " + query_string.toString());
		SolrServiceImpl solr = new SolrServiceImpl();
		DiscoverQuery dis_query = new DiscoverQuery();
		dis_query.setQuery(query_string.toString());
		dis_query.setMaxResults(limit);
		dis_query.setStart(offset);
		if(sortField!=null && sortOrder!=null){
			dis_query.setSortField(sortField, sortOrder.compareTo("asc")==0?DiscoverQuery.SORT_ORDER.asc:DiscoverQuery.SORT_ORDER.desc);
		}
		try {
			result =solr.search(context, dis_query);
			List<DSpaceObject>  list=result.getDspaceObjects();
			for(DSpaceObject obj : list){
				if(obj instanceof org.dspace.content.Item && AuthorizeManager.authorizeActionBoolean(context, obj, org.dspace.core.Constants.READ)) {
					results.add(new org.dspace.rest.common.Item((org.dspace.content.Item)obj, expand, context));
				}
			}
		} catch (SearchServiceException e) {
			log.error(e.getMessage());
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		
		return results;
	}
	
	@Override
	public ArrayList<Integer> searchIdAll(Context context, String query) {
		int limit = 1000;
		int offset = 0;
		long numResults = limit+10;
		ArrayList<Integer> results = new ArrayList<Integer>();
		StringBuilder query_string=new StringBuilder();
		query_string.append("{!lucene q.op=AND}");
		query_string.append(query);
		log.debug("search query: " + query_string.toString());
		SolrServiceImpl solr = new SolrServiceImpl();
		DiscoverQuery dis_query = new DiscoverQuery();
		dis_query.addField("search.resourcetype");
		dis_query.addField("search.resourceid");
		dis_query.addFilterQueries("search.resourcetype:2");
		dis_query.setQuery(query_string.toString());
		dis_query.setMaxResults(limit);
		while(offset-limit<=numResults){
			dis_query.setStart(offset);
			try {
				
				result =solr.search(context, dis_query);
				List<DSpaceObject>  list=result.getDspaceObjects();
				for(DSpaceObject obj : list){
					if(obj instanceof org.dspace.content.Item && AuthorizeManager.authorizeActionBoolean(context, obj, org.dspace.core.Constants.READ)) {
						results.add(((org.dspace.content.Item)obj).getID());
					}
				}
				offset+=limit;
			} catch (SearchServiceException e) {
				log.error(e.getMessage());
			} catch (SQLException e) {
				log.error(e.getMessage());
			}
		}
		
		return results;
	}

	@Override
	public ArrayList<Integer> searchId(Context context,
			HashMap<String, String> searchTerms) {
		int limit = 1000;
		int offset = 0;
		long numResults = limit+10;
		ArrayList<Integer> results = new ArrayList<Integer>();
		StringBuilder query_string=new StringBuilder();
		Iterator<String> keyset= searchTerms.keySet().iterator();
		while(keyset.hasNext()){
			String key = keyset.next();
			query_string.append(key).append(":").append(searchTerms.get(key));
			if(keyset.hasNext()){
				query_string.append(" AND ");
			}
		}
		log.debug("search query: " + query_string.toString());
		SolrServiceImpl solr = new SolrServiceImpl();
		DiscoverQuery query = new DiscoverQuery();
		query.setQuery(query_string.toString());
		query.addField("search.resourcetype");
		query.addField("search.resourceid");
		query.addFilterQueries("search.resourcetype:2");
		query.setMaxResults(limit);
		
		while(offset-limit<=numResults){
			log.debug("offset " + offset + " resutls " + numResults);
			query.setStart(offset);
			try {
				result =solr.search(context, query);
				numResults = result.getTotalSearchResults();
				List<DSpaceObject>  list=result.getDspaceObjects();
				for(DSpaceObject obj : list){
					if(obj instanceof org.dspace.content.Item && AuthorizeManager.authorizeActionBoolean(context, obj, org.dspace.core.Constants.READ)) {
						results.add(((org.dspace.content.Item)obj).getID());
					}
				}
				offset+=limit;
			} catch (SearchServiceException e) {
				log.error(e.getMessage());
			} catch (SQLException e) {
				log.error(e.getMessage());
			}
		}
		
		log.debug("number of results " + numResults + " " + results.size());
		return results;

	}


}
