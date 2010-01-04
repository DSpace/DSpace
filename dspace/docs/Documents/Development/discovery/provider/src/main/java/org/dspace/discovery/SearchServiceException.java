package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * User: mdiggory
 * Date: Oct 19, 2009
 * Time: 1:30:47 PM
 */
public class SearchServiceException extends Exception {

    public SearchServiceException() {
    }

    public SearchServiceException(String s) {
        super(s);
    }

    public SearchServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SearchServiceException(Throwable throwable) {
        super(throwable);
    }
    
}
