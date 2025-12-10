/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import org.dspace.content.authority.service.ItemAuthorityService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Stefano Maffei 4Science.com
 *
 */
public class ItemAuthorityServiceImpl implements ItemAuthorityService {

    protected CustomAuthoritySolrFilter customAuthorityFilter;

    @Autowired
    protected ConfigurationService configurationService;

    /**
     * Get the solr query to be executed Priority is given to the
     * itemauthoritylookup field which contains exact names The best match term is
     * lower priority since it generates permutations of the names
     *
     * @param searchTerm the term to be searched
     * @return solr query to be executed
     */
    @Override
    public String getSolrQueryExactMatch(String searchTerm) {
        return getSolrQuery(searchTerm);
    }

    @Override
    public String getSolrQuery(String searchTerm) {
        return customAuthorityFilter.getSolrQuery(searchTerm);
    }

    @Override
    public int getConfidenceForChoices(Choice... choices) {
        return customAuthorityFilter.getConfidenceForChoices(choices);
    }

    public CustomAuthoritySolrFilter getCustomAuthorityFilter() {
        return customAuthorityFilter;
    }

    public void setCustomAuthorityFilter(CustomAuthoritySolrFilter customAuthorityFilter) {
        this.customAuthorityFilter = customAuthorityFilter;
    }

}