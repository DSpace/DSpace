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
 * Implementation of {@link ItemAuthorityService} that delegates query
 * generation and confidence calculation to a {@link CustomAuthoritySolrFilter}.
 *
 * * @author Stefano Maffei 4Science.com
 */
public class ItemAuthorityServiceImpl implements ItemAuthorityService {

    protected CustomAuthoritySolrFilter customAuthorityFilter;

    @Autowired
    protected ConfigurationService configurationService;

    /**
     * {@inheritDoc}
     * Returns the standard query, which is optimized for matching
     * against the 'itemauthoritylookup' field.
     */
    @Override
    public String getSolrQueryExactMatch(String searchTerm) {
        return getSolrQuery(searchTerm);
    }

    /**
     * {@inheritDoc}
     * Delegates to the filter to apply parsing (e.g., Lastname/Firstname)
     * and Solr boosting weights.
     */
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