/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * Interface for customizing the Solr query generation and confidence calculation
 * for authority lookups.
 * <p>
 * Implementations define how search terms are converted into Solr queries
 * and how confidence values are assigned to the returned choices.
 * </p>
 * <p>
 * For example, a strict match implementation might:
 * <ul>
 *   <li>Convert "John Smith" to query the bestMatch field specifically</li>
 *   <li>Return CF_ACCEPTED for single exact matches</li>
 *   <li>Return CF_UNCERTAIN for multiple matches</li>
 * </ul>
 * </p>
 *
 * @author Stefano Maffei 4Science.com
 * @see ItemAuthorityServiceImpl
 */
public interface CustomAuthoritySolrFilter {

    /**
     * Converts a search term into a Solr query for authority lookups.
     *
     * @param searchTerm the text entered by the user to search for
     * @return the Solr query string to execute
     */
    public String getSolrQuery(String searchTerm);

    /**
     * Calculates the given the confidence level for authority choices.
     *
     * @param choices the authority choices to evaluate
     * @return confidence value (e.g., {@link Choices#CF_ACCEPTED}, {@link Choices#CF_UNCERTAIN}, etc.)
     */
    public int getConfidenceForChoices(Choice... choices);

}
