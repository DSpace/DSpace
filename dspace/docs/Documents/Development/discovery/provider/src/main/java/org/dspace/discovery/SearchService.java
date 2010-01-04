package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * User: mdiggory
 * Date: Oct 19, 2009
 * Time: 5:35:08 AM
 */
public interface SearchService {

    String[] getFacetFields();
        
    QueryResponse search(SolrQuery query) throws SearchServiceException;

    DSpaceObject findDSpaceObject(Context context, SolrDocument doc) throws SQLException;

    List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery);

    List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery);

}
