/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;
import static org.dspace.discovery.SolrServiceBestMatchIndexingPlugin.PUNCT_CHARS_REGEX;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.discovery.SolrServiceBestMatchIndexingPlugin;

/**
 * Implementation of {@link CustomAuthoritySolrFilter} for custom Solr filtering in Item Authority lookups.
 * <p>
 * This class constructs a complex Solr query to find matching authority entries.
 * It provides a weighted search mechanism that prioritizes exact matches and
 * prefix matches over general keyword matches.
 * </p>
 *
 * @author Stefano Maffei 4Science.com
 */

public class ItemAuthorityCustomSolrFilterImpl implements CustomAuthoritySolrFilter {

    /**
     * Constructs a weighted Solr query using the Lucene query parser.
     * <p>
     * <strong>Solr Syntax Breakdown:</strong>
     * <ul>
     * <li>{@code {!lucene ...}}: Local Parameters syntax that forces Solr to use the Lucene Query Parser.</li>
     * <li>{@code q.op=AND}: Sets the default boolean operator to AND.</li>
     * <li>{@code df=itemauthoritylookup}: Sets the default search field to 'itemauthoritylookup'.</li>
     * <li>{@code ^100}, {@code ^10}: Boosting factors. Matches in the prefix/exact terms
     * are weighted 10x higher than the "coarse best match".</li>
     * </ul>
     * <strong>Query Logic:</strong>
     * The query combines:
     * 1. A prefix match (searchTerm*) boosted by 100.
     * 2. An exact phrase match ("searchTerm") boosted by 100.
     * 3. A coarse "best match" lookup targeting a specific index field, weighted at 10.
     * </p>
     *
     * @param searchTerm the raw search string entered by the user
     * @return a fully formatted Solr query string
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

    /**
     * Generates a "coarse" query targeting the best-match index field.
     * <p>
     * This method prepares the search term by normalizing whitespace and optionally
     * removing punctuation via {@code PUNCT_CHARS_REGEX}. It targets the
     * {@link SolrServiceBestMatchIndexingPlugin#BEST_MATCH_INDEX} field.
     * </p>
     *
     * @param searchTerm         the raw search string to be processed
     * @param isSkipPunctuation  if {@code true}, regex-based punctuation removal is applied
     * @return a Solr query fragment in the format {@code field:term}
     */
    public String generateSearchQueryCoarseBestMatch(String searchTerm,
        boolean isSkipPunctuation) {
        searchTerm = StringUtils.normalizeSpace(searchTerm.replaceAll(PUNCT_CHARS_REGEX, " "));
        return SolrServiceBestMatchIndexingPlugin.BEST_MATCH_INDEX + ":" + escapeQueryChars(searchTerm);
    }

    @Override
    public int getConfidenceForChoices(Choice... choices) {
        if (choices.length == 0) {
            return Choices.CF_UNSET;
        }
        if (choices.length == 1) {
            return Choices.CF_UNCERTAIN;
        }
        return Choices.CF_AMBIGUOUS;
    }
}
