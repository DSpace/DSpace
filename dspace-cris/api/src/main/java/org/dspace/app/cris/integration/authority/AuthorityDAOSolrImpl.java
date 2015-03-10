/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.content.authority.IAuthorityDAOSolr;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;

public class AuthorityDAOSolrImpl implements IAuthorityDAOSolr
{

    private SearchService searchService;

    @Override
    public SolrDocumentList getPendingMatch(String metadata, String authority)
            throws SearchServiceException
    {
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery("{!field f=search.resourcetype}" + Constants.ITEM,
                "{!field f=authority." + RPAuthority.RP_AUTHORITY_NAME + "." 
                        + metadata + ".pending}" + authority,
                "NOT(withdrawn:true)");
        query.setFields("search.resourceid", "search.resourcetype");
        query.setRows(Integer.MAX_VALUE);

        QueryResponse response = searchService.search(query);

        return response.getResults();
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

}

