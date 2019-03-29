/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ws.Criteria;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.ws.marshaller.Marshaller;
import org.dspace.app.cris.ws.marshaller.MarshallerDynamicObject;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.jdom.Element;

public abstract class AWSServices <T> implements
		IWSService {

	private static Logger log = Logger.getLogger(AWSServices.class);

	private String name;
	
	private Marshaller<T> marshaller;

	private CrisSearchService searchServices;
	
	public String getName()
    {
        return name;
    }
	
	void setName(String name)
    {
        this.name = name;
    }
	
	public void setMarshaller(Marshaller<T> marshaller) {
		this.marshaller = marshaller;
	}
	
	public Marshaller<T> getMarshaller()
	{
	    return marshaller;
	}

	protected abstract void internalBuildFieldList(SolrQuery solrQuery,
			String... projection);

	protected abstract List<T> getWSObject(QueryResponse response);
	
	@Override
	public Element marshall(String query, String paginationStart,
			String paginationLimit, String[] splitProjection, String type,
			Element root, User userWS, String nameRoot, String sort, String sortOrder, String parent)
			throws SearchServiceException, IOException {

		SolrQuery solrQuery = buildQuery(query, 
				paginationStart.trim(), paginationLimit.trim(),
				userWS, type, sort, sortOrder, parent, splitProjection);
		QueryResponse response = searchServices.search(solrQuery);
		List<T> results = getWSObject(response);
			root = getMarshaller()
					.buildResponse(results, response.getResults().getStart(),
							response.getResults().getNumFound(), type,
							splitProjection, userWS
									.isShowHiddenMetadata(), nameRoot);
		return root;
	}

	protected SolrQuery buildQuery(String query, String start,
			String limit, User userWS, String type, String sort, String sortOrder, String parent,
			String... projection) {

		SolrQuery solrQuery = new SolrQuery();
		int resource_type = getSupportedType();

		solrQuery.setQuery(query);
		solrQuery.setStart(Integer.parseInt(start));
		solrQuery.setRows(Integer.parseInt(limit));
		
		if(StringUtils.isNotBlank(sort)) {
			
	        if (sortOrder == null || "DESC".equalsIgnoreCase(sortOrder))
	        {
	            solrQuery.setSort(sort, ORDER.desc);
	        }
	        else
	        {
	        	solrQuery.setSort(sort, ORDER.asc);
	        }
			
		}
		
		internalBuildFieldList(solrQuery, projection);

		solrQuery.addFilterQuery("search.resourcetype:" + resource_type);
		for (Criteria criteria : userWS.getCriteria()) {
			if (type.equals(criteria.getCriteria())) {
				// parse criteria follow solr form:
				// "fq=type:bllababl&fq=text:balglballlbab"
				List<String> result = new ArrayList<String>();
				if (criteria.getFilter() != null
						&& !criteria.getFilter().isEmpty()) {
					// Split
					String[] fqs = criteria.getFilter().split("&");
					for (String fq : fqs) {
						// remove prefix
						String newfq = fq.replaceFirst("fq=", "");
						result.add(newfq); // add to tmp list
					}
					// add fq
					solrQuery.addFilterQuery(result.toArray(new String[result
							.size()]));
				}
			}
		}
		return solrQuery;
	}

	protected abstract int getSupportedType();

    public void setSearchServices(CrisSearchService searchServices) {
		this.searchServices = searchServices;
	}

	public CrisSearchService getSearchServices() {
		return searchServices;
	}

}
