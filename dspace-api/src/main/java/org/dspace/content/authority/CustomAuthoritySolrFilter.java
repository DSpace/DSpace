/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 *
 * @author Stefano Maffei 4Science.com
 */
public interface CustomAuthoritySolrFilter {

    /**
     * Returns the solr query to be used for a specified authority
     *
     * @return String the solr query
     */
    public String getSolrQuery(String searchTerm);

    /**
     * Get the confidence value for the generated choices
     * @return            solr query
     */
    public int getConfidenceForChoices(Choice... choices);

}
