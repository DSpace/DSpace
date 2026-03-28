/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * Person authority filter implementation using a "coarse" matching strategy.
 * <p>
 * This class uses the exact same query logic as its parent {@link ItemAuthorityCustomSolrFilterImpl},
 * providing a general-purpose search suitable for person lookups when name structure parsing is not required.
 * </p>
 *
 * <h3>Why This Class Exists</h3>
 * <p>
 * While {@link PersonAuthoritySolrFilterImpl} parses names into first/last components and
 * {@link PersonStrictCustomSolrFilterImpl} applies strict name normalization policies,
 * this implementation provides a simpler "coarse" approach that:
 * </p>
 * <ul>
 * <li>Treats the search term as a single string (no name parsing)</li>
 * <li>Applies the standard 3-part query: prefix match, exact match, and coarse best-match</li>
 * <li>Is more forgiving than strict matching but less sophisticated than structured name parsing</li>
 * </ul>
 *
 * <h3>Query Strategy</h3>
 * <p>
 * Inherits the parent's 3-part weighted query:
 * </p>
 * <pre>
 * {!lucene q.op=AND df=itemauthoritylookup}
 *   (searchTerm*)^100                    // Prefix match
 *   OR ("searchTerm")^100                // Exact phrase
 *   OR (coarse_best_match_field:term)^10 // Normalized best-match field
 * </pre>
 *
 * <h3>Use Cases</h3>
 * <ul>
 * <li>Person lookups where name structure is unknown or unreliable</li>
 * <li>Searches that may contain nicknames, aliases, or non-standard formats</li>
 * <li>General-purpose person authority searching without strict validation</li>
 * </ul>
 *
 * @author Stefano Maffei 4Science.com
 * @see ItemAuthorityCustomSolrFilterImpl
 * @see PersonAuthoritySolrFilterImpl
 * @see PersonStrictCustomSolrFilterImpl
 */
public class PersonCoarseCustomSolrFilterImpl extends ItemAuthorityCustomSolrFilterImpl {

    /**
     * Constructs a Solr query using the parent class's coarse matching strategy.
     * <p>
     * This implementation does not override any logic - it explicitly uses the parent's
     * {@link ItemAuthorityCustomSolrFilterImpl#getSolrQuery(String)} method, which:
     * </p>
     * <ol>
     * <li>Converts search term to lowercase</li>
     * <li>Escapes special Solr characters</li>
     * <li>Creates a 3-part query with prefix, exact, and best-match components</li>
     * <li>Normalizes spaces in the query</li>
     * </ol>
     *
     * @param searchTerm the raw search string (e.g., "John Smith", "Smith, J.", "johnny")
     * @return a Solr query string identical to what the parent class would generate
     * @see ItemAuthorityCustomSolrFilterImpl#getSolrQuery(String)
     */
    @Override
    public String getSolrQuery(String searchTerm) {
        String luceneQuery = ClientUtils.escapeQueryChars(searchTerm.toLowerCase()) + "*";
        String solrQuery = null;
        luceneQuery = luceneQuery.replaceAll("\\\\ ", " ");
        String subLuceneQuery = luceneQuery.substring(0,
            luceneQuery.length() - 1);
        solrQuery = "{!lucene q.op=AND df=itemauthoritylookup}("
            + luceneQuery
            + ")^100 OR (\""
            + subLuceneQuery + "\")^100 OR "
            + "(" + generateSearchQueryCoarseBestMatch(searchTerm, true) + ")^10 ";

        return solrQuery;
    }



}
