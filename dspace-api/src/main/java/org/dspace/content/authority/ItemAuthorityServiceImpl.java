/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.authority.service.ItemAuthorityService;

public class ItemAuthorityServiceImpl implements ItemAuthorityService {

    @Override
    public String getSolrQuery(String searchTerm) {
        String luceneQuery = ClientUtils.escapeQueryChars(searchTerm.toLowerCase()) + "*";
        String solrQuery = null;
        luceneQuery = luceneQuery.replaceAll("\\\\ "," ");
        String subLuceneQuery = luceneQuery.substring(0,
                luceneQuery.length() - 1);
        solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}("
                        + luceneQuery
                        + ") OR (\""
                        + subLuceneQuery + "\")^2 OR "
                        + "(itemauthoritylookupexactmatch:\"" + subLuceneQuery + "\")^10 ";

        return solrQuery;
    }

}