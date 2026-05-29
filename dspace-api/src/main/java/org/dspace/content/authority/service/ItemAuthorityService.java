/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import org.dspace.content.authority.Choice;

/**
 * Service interface for managing Item-based authority control.
 * Provides methods to generate search queries and evaluate confidence
 * levels for authority choices linked to DSpace Items.
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @author Stefano Maffei 4Science.com
 */
public interface ItemAuthorityService {

    /**
     * Generates a Solr query for exact matching. Used when the search
     * term is expected to match a specific, unique identifier or
     * normalized field.
     *
     * @param searchTerm The search term to match exactly
     * @return the Solr query string for exact matches
     */
    public String getSolrQueryExactMatch(String searchTerm);

    /**
     * Generates a Solr query for discovery. This typically includes
     * logic for name parsing, boosting, and handling permutations
     * of the search term.
     *
     * @param searchTerm The search term string
     * @return the general search Solr query string
     */
    public String getSolrQuery(String searchTerm);

    /**
     * Evaluates a set of choices to determine the appropriate authority
     * confidence value.
     *
     * @param choices The choices to evaluate
     * @return the confidence value (e.g., CF_ACCEPTED, CF_AMBIGUOUS)
     */
    public int getConfidenceForChoices(Choice... choices);

}