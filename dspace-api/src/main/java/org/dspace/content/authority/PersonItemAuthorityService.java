package org.dspace.content.authority;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DCPersonName;
import org.dspace.content.authority.service.ItemAuthorityService;

public class PersonItemAuthorityService implements ItemAuthorityService {

	@Override
	public String getSolrQuery(String searchTerm) {
        DCPersonName tmpPersonName = new DCPersonName(searchTerm.toLowerCase());
        
		String solrQuery = null;
		if (StringUtils.isBlank(tmpPersonName.getFirstNames())) {
			String luceneQuery = ClientUtils.escapeQueryChars(tmpPersonName.getLastName().trim())
					+ (StringUtils.isNotBlank(tmpPersonName.getFirstNames()) ? "" : "*");
			luceneQuery = luceneQuery.replaceAll("\\\\ ", " ");

			solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}(" + luceneQuery + ") OR (\""
					+ luceneQuery.substring(0, luceneQuery.length() - 1) + "\")";
		} else {
			String luceneQuerySurExact = ClientUtils.escapeQueryChars(tmpPersonName.getLastName().trim()) + " "
					+ ClientUtils.escapeQueryChars(tmpPersonName.getFirstNames().trim()) + "*";
			
			luceneQuerySurExact = luceneQuerySurExact.replaceAll("\\\\ ", " "); 
			String luceneQuerySurJolly = ClientUtils.escapeQueryChars(tmpPersonName.getLastName().trim()) + "* "
					+ ClientUtils.escapeQueryChars(tmpPersonName.getFirstNames().trim()) + "*";
			
			solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}(" + luceneQuerySurExact + ") OR (\""
					+ luceneQuerySurExact.substring(0, luceneQuerySurExact.length() - 1) + "\") OR (" 
					+ luceneQuerySurJolly + ") OR ("
					+ luceneQuerySurJolly.substring(0, luceneQuerySurJolly.length() - 1) + ")";
		}
		
		return solrQuery;
	}
	
}