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

public class AuthorityNameAuthorityFilter extends CustomAuthorityFilter {

    private List<String> supportedAuthorities;

    public boolean appliesTo(LinkableEntityAuthority linkableEntityAuthority) {
        return CollectionUtils.isEmpty(supportedAuthorities)
            || supportedAuthorities.contains(linkableEntityAuthority.getPluginInstanceName());
    }

    public AuthorityNameAuthorityFilter(List<String> customQueries) {
        super.customQueries = customQueries;
    }

    public void setSupportedAuthorities(List<String> supportedAuthorities) {
        this.supportedAuthorities = supportedAuthorities;
    }

}
