/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openstar.service;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.deduplication.service.dto.OpenstarDto;
import org.dspace.util.SolrImportExportException;

public interface OpenstarService {

	
	public void query();
	
	public void delete();
	
	public void commit();

	public void store(List<OpenstarDto> chunk) throws SolrImportExportException,
	    SolrServerException, IOException;
	
/*
 
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
*/
}
