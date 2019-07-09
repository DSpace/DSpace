/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

/**
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
public interface ItemAuthorityService {

    /**
     * Get solr query
     * @param searchTerm The search term string
     * @return solr query
     */
    public String getSolrQuery(String searchTerm);

}