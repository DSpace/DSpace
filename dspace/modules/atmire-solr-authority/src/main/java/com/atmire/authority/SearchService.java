package com.atmire.authority;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 7-dec-2010
 * Time: 10:13:07
 */
public interface SearchService {

    QueryResponse search(SolrQuery query) throws Exception;


}
