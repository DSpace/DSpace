package org.dspace.content.authority;

import org.apache.solr.common.SolrDocumentList;
import org.dspace.discovery.SearchServiceException;

public interface IAuthorityDAOSolr
{
    public SolrDocumentList getPendingMatch(String metadata, String authority) throws SearchServiceException;
}