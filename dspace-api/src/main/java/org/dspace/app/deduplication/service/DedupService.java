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
 *
 * @author 4Science
 */
public interface DedupService {

    /**
     * Index a Solr deduplication record for the given item
     * @param ctx   DSpace context
     * @param item  DSpace item
     * @param force Force index?
     * @throws SearchServiceException
     */
    void indexContent(Context ctx, Item item, boolean force) throws SearchServiceException;

    /**
     * Remove the Solr deduplication record for the given item
     * @param context
     * @param item
     */
    void unIndexContent(Context context, Item item);

    /**
     * Find all deduplication records for the given query and filters
     * @param query     Solr query string
     * @param filters   One or more filter strings
     * @return          Solr query response
     * @throws SearchServiceException
     */
    QueryResponse find(String query, String... filters) throws SearchServiceException;

    /**
     * Delete all deduplication Solr records for a given query string
     * @param query Query string
     * @return      Solr update response
     * @throws SearchServiceException
     */
    UpdateResponse delete(String query) throws SearchServiceException;

    /**
     * Clean the entire Solr deduplication index
     * @param force
     * @throws IOException
     * @throws SQLException
     * @throws SearchServiceException
     */
    void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException;

    /**
     * Index a Solr deduplication record for the given item
     * @param context   DSpace context
     * @param ids       List of DSpace item IDs to index
     * @param force     Force index?
     * @throws SearchServiceException
     */
    void indexContent(Context context, List<UUID> ids, boolean force);

    /**
     * Update (commit) the deduplication Solr index
     * @param context   DSpace context
     * @param force
     */
    void updateIndex(Context context, boolean force);

    /**
     * Optimize the deduplication Solr index
     */
    void optimize();

    /**
     * Remove the Solr deduplication record for the given item by handle or UUID
     * @param context       DSpace context
     * @param handleOrUuid  Handle or UUID of the item
     */
    void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException;

    /**
     * Remove the Solr deduplication record for the given item by UUID
     * @param context   DSpace context
     * @param id        UUID of the item
     */
    void unIndexContent(Context context, UUID id) throws IllegalStateException, SQLException;

    /**
     * Search the Solr deduplication index with the given Solr query
     * @param solrQuery Solr query object
     * @return          Solr query response
     * @throws SearchServiceException
     */
    QueryResponse search(SolrQuery solrQuery) throws SearchServiceException;

    /**
     * Build and index a document representing a Solr document for the given item IDs, deduplication flag and note
     *
     * @param context   DSpace context
     * @param firstItemID   First DSpace item ID
     * @param secondItemID  Second DSpace item ID
     * @param flag      Deduplication flag indicating decision type and value (eg verify_ws)
     * @param note      Decision note
     */
    void buildDecision(Context context, UUID firstItemID, UUID secondItemID, DeduplicationFlag flag, String note);

    /**
     * Commit changes to the Solr deduplication index
     */
    void commit();

    /**
     * Remove a decision Solr record for the given item IDs and decision type
     * @param firstId   First DSpace item ID
     * @param secondId  Second DSpace item ID
     * @param type      Decision type
     * @throws SearchServiceException
     */
    void removeStoredDecision(UUID firstId, UUID secondId, DuplicateDecisionType type)
            throws SearchServiceException;

    /**
     * Find decision Solr records for the given item IDs and decision type
     * @param firstItemID   First DSpace item ID
     * @param secondItemID  Second DSpace item ID
     * @param type      Decision type
     * @return
     * @throws SearchServiceException
     */
    QueryResponse findDecisions(UUID firstItemID, UUID secondItemID, DuplicateDecisionType type)
            throws SearchServiceException;
}
