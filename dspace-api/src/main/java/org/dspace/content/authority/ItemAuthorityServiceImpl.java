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