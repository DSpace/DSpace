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
 * Filter implementation that targets specific DSpace Entity Types.
 * <p>
 * This class allows for the conditional application of custom Solr filter queries
 * during authority lookups. It checks the linked entity type of the authority
 * (e.g., "Person", "OrgUnit") against a whitelist defined in the
 * {@code supportedEntities} property.
 * </p>
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class EntityTypeAuthorityFilter extends CustomAuthorityFilter {

    private List<String> supportedEntities;

    public void setSupportedEntities(List<String> supportedEntities) {
        this.supportedEntities = supportedEntities;
    }

    /**
     * Determines if the filter applies to the given authority.
     * @param linkableEntityAuthority  the authority to check.
     * @return                         true if supportedEntities is empty (applies to all) or if the
     *                                 authority's entity type is explicitly listed in supportedEntities.
     */
    public boolean appliesTo(LinkableEntityAuthority linkableEntityAuthority) {

        return CollectionUtils.isEmpty(supportedEntities)
            || supportedEntities.contains(linkableEntityAuthority.getLinkedEntityType());
    }

    public EntityTypeAuthorityFilter(List<String> customQueries) {
        super.customQueries = customQueries;
    }
}
