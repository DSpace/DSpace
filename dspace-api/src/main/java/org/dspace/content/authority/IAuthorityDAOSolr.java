/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.solr.common.SolrDocumentList;
import org.dspace.discovery.SearchServiceException;

public interface IAuthorityDAOSolr
{
    public SolrDocumentList getPendingMatch(String metadata, String authority) throws SearchServiceException;
}