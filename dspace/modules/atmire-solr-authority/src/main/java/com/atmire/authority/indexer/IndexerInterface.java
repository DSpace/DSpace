package com.atmire.authority.indexer;

import org.apache.solr.common.SolrInputField;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 10-dec-2010
 * Time: 15:16:39
 * Modified : Fabio March/2011
 */
public interface IndexerInterface {
    public void init();
    public Map<String, String> nextValue();
    public boolean hasMore();
    public void close();
    public String indexerName();
    public String getSource();
    public Map<String, String> createHashMap(String fieldName, String value);
}
