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
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @author Stefano Maffei 4Science.com
 */
public interface ItemAuthorityService {

    /**
     * Get solr query
     * @param  searchTerm The search term string
     * @return            solr query
     */
    public String getSolrQueryExactMatch(String searchTerm);

    /**
     * Get solr query
     * @param  searchTerm The search term string
     * @return            solr query
     */
    public String getSolrQuery(String searchTerm);

    /**
     * Get the confidence value for the generated choices
     * @return            solr query
     */
    public int getConfidenceForChoices(Choice... choices);

}