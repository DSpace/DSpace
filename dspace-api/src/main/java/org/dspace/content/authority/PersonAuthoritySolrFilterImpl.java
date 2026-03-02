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
 * Specialized implementation of {@link ItemAuthorityCustomSolrFilterImpl} for Person authority lookups.
 * <p>
 * This class extends the base authority filter to provide person-name-aware searching.
 * Unlike the parent class which treats search terms as simple strings, this implementation:
 * </p>
 * <ul>
 * <li>Parses search terms using {@link DCPersonName} to extract first and last names</li>
 * <li>Constructs different weighted Solr queries depending on whether both first and last names are provided</li>
 * <li>Provides higher precision for person name matching by applying name-specific search patterns</li>
 * </ul>
 *
 * <h3>Query Logic Differences from Parent Class</h3>
 * <p>
 * <strong>Parent class ({@code ItemAuthorityCustomSolrFilterImpl}):</strong>
 * <ul>
 * <li>Simple 3-part query: prefix match (^100) + exact phrase (^100) + coarse best-match (^10)</li>
 * <li>Treats entire search term as a single string</li>
 * </ul>
 * </p>
 * <p>
 * <strong>This class ({@code PersonAuthoritySolrFilterImpl}):</strong>
 * <ul>
 * <li><strong>Last name only:</strong> 2-part query with wildcard suffix matching (^100) + exact phrase (^10)</li>
 * <li><strong>Full name:</strong> 4-part query with various combinations of exact/wildcard matches
 * on both first and last names, weighted 40-100</li>
 * <li>Does NOT use the coarse best-match lookup from the parent class</li>
 * </ul>
 * </p>
 *
 * @author Stefano Maffei 4Science.com
 * @see ItemAuthorityCustomSolrFilterImpl
 * @see DCPersonName
 */

public class PersonAuthoritySolrFilterImpl extends ItemAuthorityCustomSolrFilterImpl {

    /**
     * Constructs a person-name-aware Solr query with weighted name matching patterns.
     * <p>
     * This method overrides the parent implementation to provide specialized handling for person names.
     * The search term is parsed into first and last name components using {@link DCPersonName}.
     * </p>
     *
     * <h3>Query Construction Logic</h3>
     * <p>
     * <strong>Case 1: Last name only (no first name provided)</strong>
     * </p>
     * <pre>
     * {!lucene q.op=AND df=itemauthoritylookup}
     *   (lastName*)^100              // Prefix match on last name
     *   OR ("lastName")^10           // Exact phrase match on last name
     * </pre>
     * <p>
     * Example: "smith" → matches "Smith", "Smithson", "Smith-Jones"
     * </p>
     *
     * <p>
     * <strong>Case 2: Full name provided (first + last)</strong>
     * </p>
     * <pre>
     * {!lucene q.op=AND df=itemauthoritylookup}
     *   (lastName firstName*)^90           // Exact last, prefix first
     *   OR ("lastName firstName")^100      // Exact phrase for both
     *   OR (lastName* firstName*)^40       // Prefix match on both
     *   OR (lastName* firstName)^50        // Prefix last, exact first
     * </pre>
     * <p>
     * Example: "smith john" →
     * <ul>
     * <li>"Smith John" (exact) gets score 100</li>
     * <li>"Smith Johnny" (firstName prefix) gets score 90</li>
     * <li>"Smith-Jones John" (lastName prefix) gets score 50</li>
     * <li>"Smithson Johnny" (both prefix) gets score 40</li>
     * </ul>
     * </p>
     *
     * <h3>Special Handling</h3>
     * <ul>
     * <li>All input is converted to lowercase for case-insensitive matching</li>
     * <li>Special characters are escaped using {@link ClientUtils#escapeQueryChars(String)}</li>
     * <li>Escaped spaces ({@code "\ "}) are converted back to regular spaces for natural phrase matching</li>
     * </ul>
     *
     * @param searchTerm the raw search string entered by the user (e.g., "Smith, John" or "John Smith")
     * @return a fully formatted Solr query string with weighted person name matching
     */
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
