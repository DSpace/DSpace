/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Service used to handle indexing related to the dedup solr core.
 */
public interface DedupService {
    public void indexContent(Context ctx, Item item, boolean force) throws SearchServiceException;

    public void unIndexContent(Context context, Item item);

    public QueryResponse find(String query, String... filters) throws SearchServiceException;

    public UpdateResponse delete(String query) throws SearchServiceException;

    public void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException;

    public void indexContent(Context context, List<UUID> ids, boolean force);

    public void updateIndex(Context context, boolean b);

    public void optimize();

    public void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException;

    public void unIndexContent(Context context, UUID id) throws IllegalStateException, SQLException;

    public QueryResponse search(SolrQuery solrQuery) throws SearchServiceException;

    public void buildDecision(Context context, UUID firstId, UUID secondId, DeduplicationFlag flag, String note);

    public void commit();

    public void removeStoredDecision(UUID firstId, UUID secondId, DuplicateDecisionType type)
            throws SearchServiceException;

    public QueryResponse findDecisions(UUID firstItemID, UUID secondItemID, DuplicateDecisionType t)
            throws SearchServiceException;
}
