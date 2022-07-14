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
    void indexContent(Context ctx, Item item, boolean force) throws SearchServiceException;

    void unIndexContent(Context context, Item item);

    QueryResponse find(String query, String... filters) throws SearchServiceException;

    UpdateResponse delete(String query) throws SearchServiceException;

    void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException;

    void indexContent(Context context, List<UUID> ids, boolean force);

    void updateIndex(Context context, boolean b);

    void optimize();

    void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException;

    void unIndexContent(Context context, UUID id) throws IllegalStateException, SQLException;

    QueryResponse search(SolrQuery solrQuery) throws SearchServiceException;

    void buildDecision(Context context, UUID firstId, UUID secondId, DeduplicationFlag flag, String note);

    void commit();

    void removeStoredDecision(UUID firstId, UUID secondId, DuplicateDecisionType type)
            throws SearchServiceException;

    QueryResponse findDecisions(UUID firstItemID, UUID secondItemID, DuplicateDecisionType t)
            throws SearchServiceException;
}
