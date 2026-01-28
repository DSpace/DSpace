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

public class EntityTypeAuthorityFilter extends CustomAuthorityFilter {

    private List<String> supportedEntities;

    public void setSupportedEntities(List<String> supportedEntities) {
        this.supportedEntities = supportedEntities;
    }

    public boolean appliesTo(LinkableEntityAuthority linkableEntityAuthority) {

        return CollectionUtils.isEmpty(supportedEntities)
            || supportedEntities.contains(linkableEntityAuthority.getLinkedEntityType());
    }

    public EntityTypeAuthorityFilter(List<String> customQueries) {
        super.customQueries = customQueries;
    }
}
