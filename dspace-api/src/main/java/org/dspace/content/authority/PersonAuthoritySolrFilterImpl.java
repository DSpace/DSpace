/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DCPersonName;

/**
 *
 * @author Stefano Maffei 4Science.com
 *
 */

public class PersonAuthoritySolrFilterImpl extends ItemAuthorityCustomSolrFilterImpl {

    @Override
    public String getSolrQuery(String searchTerm) {
        DCPersonName tmpPersonName = new DCPersonName(searchTerm.toLowerCase());

        String solrQuery = null;
        String lastName = ClientUtils.escapeQueryChars(tmpPersonName.getLastName().trim());
        String firstName = ClientUtils.escapeQueryChars(tmpPersonName.getFirstNames().trim());

        if (StringUtils.isBlank(firstName)) {
            String luceneQuery = lastName + "*";
            luceneQuery = luceneQuery.replaceAll("\\\\ ", " ");

            solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}(" + luceneQuery + ")^100 OR (\""
                + luceneQuery.substring(0, luceneQuery.length() - 1) + "\")^10";
        } else {
            String luceneQuerySurExact = lastName + " " + firstName + "*";

            luceneQuerySurExact = luceneQuerySurExact.replaceAll("\\\\ ", " ");
            String luceneQuerySurJolly = lastName + "* " + firstName + "*";

            solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}("
                + luceneQuerySurExact + ")^90 OR (\""
                + luceneQuerySurExact.substring(0, luceneQuerySurExact.length() - 1) + "\")^100 OR ("
                + luceneQuerySurJolly + ")^40 OR ("
                + luceneQuerySurJolly.substring(0, luceneQuerySurJolly.length() - 1) + ")^50";
        }

        return solrQuery;
    }

}
