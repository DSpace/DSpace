/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * Filter that conditionally applies custom Solr queries based on the name
 * of the authority plugin in use.
 * This is used to inject specific search constraints (e.g., filtering by
 * metadata type) into the authority resolution process for a subset of
 * configured authorities.
 *
 * @author Stefano Maffei 4Science.com
 */
public class AuthorityNameAuthorityFilter extends CustomAuthorityFilter {

    private List<String> supportedAuthorities;

    /**
     * Determines if the filter's custom queries should be applied to the
     * given authority.
     *
     * @param linkableEntityAuthority The authority instance to evaluate.
     * @return true if supportedAuthorities is empty (global application) or
     * if it contains the authority's plugin instance name.
     */
    public boolean appliesTo(LinkableEntityAuthority linkableEntityAuthority) {
        return CollectionUtils.isEmpty(supportedAuthorities)
            || supportedAuthorities.contains(linkableEntityAuthority.getPluginInstanceName());
    }

    /**
     * Creates a filter with a list of Solr query constraints.
     *
     *  @param customQueries List of Solr query strings (e.g., "dc.type:mytype").
     */
    public AuthorityNameAuthorityFilter(List<String> customQueries) {
        super.customQueries = customQueries;
    }

    /**
     * Configures which authority plugin names this filter targets.
     *
     * @param supportedAuthorities List of plugin names defined in authority-filters.xml
     */
    public void setSupportedAuthorities(List<String> supportedAuthorities) {
        this.supportedAuthorities = supportedAuthorities;
    }

}
