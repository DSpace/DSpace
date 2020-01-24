package org.dspace.app.deduplication.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public interface DedupService {
	public void indexContent(Context ctx, /*BrowsableDSpaceObject<UUID>*/DSpaceObject dso, boolean force) throws SearchServiceException;

	public void unIndexContent(Context context, /*BrowsableDSpaceObject<UUID>*/DSpaceObject dso);

	public QueryResponse find(String query, String... filters) throws SearchServiceException;

	public UpdateResponse delete(String query) throws SearchServiceException;

	public void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException;

	public void cleanIndex(boolean force, int type) throws IOException, SQLException, SearchServiceException;

	public void updateIndex(Context context, boolean force);

	public void indexContent(Context context, List<UUID> ids, boolean force, int type);

	public void updateIndex(Context context, boolean b, Integer type);

	public void optimize();

	public void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException;

	public void unIndexContent(Context context, UUID id, Integer type) throws IllegalStateException, SQLException;

	public QueryResponse search(SolrQuery solrQuery) throws SearchServiceException;

	public void buildDecision(Context context, String firstId, String secondId, Integer type, DeduplicationFlag flag,
			String note);

	public void commit();

	public void removeStoredDecision(UUID firstId, UUID secondId, DuplicateDecisionType type)
			throws SearchServiceException;

	public QueryResponse findDecisions(UUID firstItemID, UUID secondItemID, DuplicateDecisionType t)
			throws SearchServiceException;
}
