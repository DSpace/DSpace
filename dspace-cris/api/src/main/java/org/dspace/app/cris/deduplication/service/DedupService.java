/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public interface DedupService
{

    public void indexContent(Context ctx, DSpaceObject dso, boolean force) throws SearchServiceException;

    public void unIndexContent(Context context, DSpaceObject dso);

    public QueryResponse find(String query, String... filters) throws SearchServiceException;

    public UpdateResponse delete(String query) throws SearchServiceException;

    public void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException;
    
    public void cleanIndex(boolean force, int type) throws IOException, SQLException, SearchServiceException;

    public void updateIndex(Context context, boolean force);

    public void indexContent(Context context, List<Integer> ids, boolean force,
            int type);

    public void updateIndex(Context context, boolean b, Integer type);

    public void optimize();

    public void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException;
    
    public void unIndexContent(Context context, Integer id, Integer type) throws IllegalStateException, SQLException;
    
    public QueryResponse search(SolrQuery solrQuery) throws SearchServiceException;

    public void buildReject(Context context, Integer firstId, Integer secondId, Integer type,
            DeduplicationFlag flag, String note);

    public void commit();

}
