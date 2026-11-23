/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;
import static org.dspace.discovery.SolrServiceStrictBestMatchIndexingPlugin.cleanNameWithStrictPolicies;

import java.util.Optional;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.discovery.SolrServiceStrictBestMatchIndexingPlugin;

/**
 * Person authority filter implementation using a "strict" matching strategy with normalized name policies.
 * <p>
 * Unlike {@link PersonAuthoritySolrFilterImpl} (which parses names into first/last components) and
 * {@link PersonCoarseCustomSolrFilterImpl} (which uses general prefix/exact matching), this implementation
 * applies <strong>strict normalization policies</strong> to clean and standardize person names before querying.
 * </p>
 *
 * <h3>Strict Normalization Process</h3>
 * <p>
 * This class delegates name cleaning to {@link SolrServiceStrictBestMatchIndexingPlugin#cleanNameWithStrictPolicies(String)},
 * which applies strict policies such as:
 * </p>
 * <ul>
 * <li>Removing diacritics and special characters</li>
 * <li>Normalizing whitespace</li>
 * <li>Converting to lowercase</li>
 * <li>Applying consistent name formatting rules</li>
 * </ul>
 *
 * <h3>Query Strategy</h3>
 * <p>
 * Generates a <strong>single-field exact match</strong> query on the strict best-match index:
 * </p>
 * <pre>
 * strict_best_match_index:normalizedTerm
 * </pre>
 * <p>
 * Example: "O'Brien, Seán" → "obrien sean" → {@code strict_best_match_index:obrien\ sean}
 * </p>
 *
 * <h3>Confidence Levels</h3>
 * <p>
 * This implementation returns higher confidence scores than coarse matching:
 * </p>
 * <ul>
 * <li><strong>0 results</strong>: {@link Choices#CF_UNSET}</li>
 * <li><strong>1 result</strong>: {@link Choices#CF_ACCEPTED} (higher confidence than coarse)</li>
 * <li><strong>Multiple results</strong>: {@link Choices#CF_UNCERTAIN}</li>
 * </ul>
 *
 * @author Stefano Maffei 4Science.com
 * @see SolrServiceStrictBestMatchIndexingPlugin
 * @see PersonAuthoritySolrFilterImpl
 * @see PersonCoarseCustomSolrFilterImpl
 */
public class PersonStrictCustomSolrFilterImpl implements CustomAuthoritySolrFilter {

    /**
     * Constructs a Solr query using strict name normalization.
     * <p>
     * This method delegates to {@link #generateSearchQueryStrictBestMatch(String)} to apply
     * strict policies and query the normalized index field.
     * </p>
     *
     * @param searchTerm the raw search string (e.g., "O'Brien, Seán")
     * @return a Solr query targeting the strict best-match index, or {@code null} if normalization fails
     * @see #generateSearchQueryStrictBestMatch(String)
     */
    @Override
    public String getSolrQuery(String searchTerm) {
        return generateSearchQueryStrictBestMatch(searchTerm);
    }

    /**
     * Generates a Solr query targeting the strict best-match index field.
     * <p>
     * This method applies strict normalization policies to the search term and constructs
     * a query against {@link SolrServiceStrictBestMatchIndexingPlugin#BEST_MATCH_INDEX}.
     * </p>
     *
     * <h3>Processing Steps</h3>
     * <ol>
     * <li>Apply strict normalization via {@link SolrServiceStrictBestMatchIndexingPlugin#cleanNameWithStrictPolicies(String)}</li>
     * <li>If normalization succeeds, escape the cleaned term using {@link ClientUtils#escapeQueryChars(String)}</li>
     * <li>Construct query in format: {@code strict_best_match_index:escapedTerm}</li>
     * <li>If normalization returns {@code null}, return {@code null} (no valid query)</li>
     * </ol>
     *
     * <h3>Examples</h3>
     * <pre>
     * Input: "O'Brien, Seán"
     * Normalized: "obrien sean"
     * Output: "strict_best_match_index:obrien\ sean"
     *
     * Input: "Smith-Jones, Mary-Anne"
     * Normalized: "smithjones maryanne"
     * Output: "strict_best_match_index:smithjones\ maryanne"
     *
     * Input: "" (empty or invalid)
     * Normalized: null
     * Output: null
     * </pre>
     *
     * @param searchTerm the raw search string to be normalized and queried
     * @return a Solr query string targeting the strict best-match index, or {@code null} if the
     *         search term cannot be normalized according to strict policies
     * @see SolrServiceStrictBestMatchIndexingPlugin#cleanNameWithStrictPolicies(String)
     * @see SolrServiceStrictBestMatchIndexingPlugin#BEST_MATCH_INDEX
     */
    public String generateSearchQueryStrictBestMatch(String searchTerm) {
        return Optional.ofNullable(cleanNameWithStrictPolicies(searchTerm))
            .map(query -> SolrServiceStrictBestMatchIndexingPlugin.BEST_MATCH_INDEX + ":" + escapeQueryChars(query))
            .orElse(null);
    }

    /**
     * Determines the confidence level for authority choices based on result count.
     * <p>
     * This implementation returns higher confidence than {@link PersonCoarseCustomSolrFilterImpl}
     * when a single result is found, reflecting the higher precision of strict matching.
     * </p>
     *
     * <h3>Confidence Mapping</h3>
     * <ul>
     * <li><strong>No results</strong> (length = 0): {@link Choices#CF_UNSET} - No authority data available</li>
     * <li><strong>Single result</strong> (length = 1): {@link Choices#CF_ACCEPTED} - High confidence due to strict matching</li>
     * <li><strong>Multiple results</strong> (length > 1): {@link Choices#CF_UNCERTAIN} - Ambiguous, user must choose</li>
     * </ul>
     *
     * @param choices variable number of Choice objects returned by the authority lookup
     * @return a confidence value from {@link Choices} indicating the reliability of the results
     * @see Choices#CF_UNSET
     * @see Choices#CF_ACCEPTED
     * @see Choices#CF_UNCERTAIN
     */
    @Override
    public int getConfidenceForChoices(Choice... choices) {
        if (choices.length == 0) {
            return Choices.CF_UNSET;
        }
        if (choices.length == 1) {
            return Choices.CF_ACCEPTED;
        }
        return Choices.CF_UNCERTAIN;
    }
}
