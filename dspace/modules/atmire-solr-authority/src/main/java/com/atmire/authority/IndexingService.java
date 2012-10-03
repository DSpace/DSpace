package com.atmire.authority;

import org.apache.solr.common.SolrInputField;

import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 7-dec-2010
 * Time: 10:12:54
 * Modified : Fabio March/2011
 */
public interface IndexingService {
    public void indexContent(Map<String,String> values, boolean force);
    public void cleanIndex() throws Exception;
    public void cleanIndex(String source) throws Exception;
    public void commit();
}
