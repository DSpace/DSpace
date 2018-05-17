/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;
/**
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.dspace.utils.DSpace;

/**
 * Service to interact with the Solr resourcesync core
 * 
 */
public class ResourceSyncAuditService {
	private static final String RESOURCE_ID_FIELD = "resource_id";

	private static final String RESOURCE_TYPE_FIELD = "resource_type";

	private static final String CHANGETYPE_FIELD = "changetype";

	private static final String DATETIME_FIELD = "datetime";

	private static final String SCOPES_FIELD = "scopes";
	
	private static final String HANDLE_FIELD = "handle";
	
	private static final String EXTRA_FIELD = "extra";

	// ENUM con i valori di type
	// Install|Modify_Metadata|Delete|Add|Remove
	public static enum ChangeType {
		CREATE("create"), REMOVE("remove"), UPDATE("update");
		private final String type;

		ChangeType(String type) {
			this.type = type;
		}

		public String type() {
			return type;
		}
	}

	private Logger log = LogManager.getLogger(ResourceSyncAuditService.class);

	/**
	 * Non-Static CommonsHttpSolrServer for processing indexing events.
	 */
	private HttpSolrServer solr = null;

	protected HttpSolrServer getSolr() {
		if (solr == null) {
			String solrService = new DSpace().getConfigurationService().getProperty("resourcesync.solr.server");

			try {
				log.debug("Solr URL: " + solrService);
				solr = new HttpSolrServer(solrService);

				solr.setBaseURL(solrService);
				solr.setUseMultiPartPost(true);
				SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
				solrQuery.setFields(RESOURCE_ID_FIELD, RESOURCE_TYPE_FIELD, CHANGETYPE_FIELD, DATETIME_FIELD,
						SCOPES_FIELD);
				solrQuery.setRows(1);
				solr.query(solrQuery);
			} catch (SolrServerException e) {
				log.error("Error while initializing solr server", e);
			}
		}
		return solr;
	}

	public void addEvent(UUID resourceID, int resourcetype, ChangeType eventtype, Date date, List<String> scopes,String handle,List<String> identifiers) {
		SolrInputDocument solrInDoc = new SolrInputDocument();
		solrInDoc.addField(RESOURCE_ID_FIELD, resourceID);
		solrInDoc.addField(RESOURCE_TYPE_FIELD, resourcetype);
		solrInDoc.addField(CHANGETYPE_FIELD, eventtype);
		solrInDoc.addField(DATETIME_FIELD, date);
		solrInDoc.addField(SCOPES_FIELD, scopes);
		solrInDoc.addField(HANDLE_FIELD, handle);
		solrInDoc.addField(EXTRA_FIELD, identifiers);
		try {
			getSolr().add(solrInDoc);
		} catch (SolrServerException | IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public List<ResourceSyncEvent> listEvents(Date from, Date to, String scope) {
//		SolrQuery solrQuery = new SolrQuery(buildTimeQuery(from, to));
		SolrQuery solrQuery = new SolrQuery((buildTimeQuery(from, to)));
		solrQuery.setRows(Integer.MAX_VALUE);
		solrQuery.addSort(new SortClause(DATETIME_FIELD, ORDER.asc));
		solrQuery.addFilterQuery(SCOPES_FIELD+":" + scope);
//		solrQuery.addFilterQuery("scope:" + scope);

		QueryResponse queryResponse;
		try {
			queryResponse = getSolr().query(solrQuery);
		} catch (SolrServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		List<ResourceSyncEvent> listResourceSyncEvent = new ArrayList<ResourceSyncEvent>();
		// queryResponse.getResults().iterator().next().get(RESOURCE_ID_FIELD);
		Iterator<SolrDocument> iterator = queryResponse.getResults().iterator();
		while (iterator.hasNext()) {
			ResourceSyncEvent rse = new ResourceSyncEvent();
			SolrDocument sd = iterator.next();
			rse.setResource_id((UUID) sd.getFieldValue(RESOURCE_ID_FIELD));
			rse.setResource_type((int) sd.getFieldValue(RESOURCE_TYPE_FIELD));
			rse.setChangetype((String) sd.getFieldValue(CHANGETYPE_FIELD));
			rse.setDatetime((Date) sd.getFieldValue(DATETIME_FIELD));
			rse.setScopes((List<String>) sd.getFieldValue(SCOPES_FIELD));
			rse.setHandle((String)sd.getFieldValue(HANDLE_FIELD));
			rse.setScopes((List<String>) sd.getFieldValue(EXTRA_FIELD));
//			System.out.println("+++Resource id = "+rse.getResource_id()+" Resource Type = "+
//					rse.getResource_type()+" changeType = "+rse.getChangetype()+ " datetime = "+rse.getDatetime()+
//						" scopes = "+rse.getScopes());
			listResourceSyncEvent.add(rse);
		}
		return listResourceSyncEvent;
	}

	private String buildTimeQuery(Date from, Date to) {
		String fromDate;
		if (from == null) {
			fromDate = "*";
		} else {
			fromDate = DateUtil.getThreadLocalDateFormat().format(from);
		}
		String toDate;
		if (to == null) {
			toDate = "*";
		} else {
			toDate = DateUtil.getThreadLocalDateFormat().format(to);
		}
		return DATETIME_FIELD + ":[" + fromDate + " TO " + toDate + "]";
	}
}
