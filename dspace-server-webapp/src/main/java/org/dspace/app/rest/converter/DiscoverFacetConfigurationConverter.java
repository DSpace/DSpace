/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to convert an object of the type DiscoveryConfiguration into a FacetConfigurationRest object
 */
@Component
public class DiscoverFacetConfigurationConverter {
    public FacetConfigurationRest convert(final String configurationName, final String scope,
                                          DiscoveryConfiguration configuration) {
        FacetConfigurationRest facetConfigurationRest = new FacetConfigurationRest();

        facetConfigurationRest.setConfiguration(configurationName);
        facetConfigurationRest.setScope(scope);

        if (configuration != null) {
            addSidebarFacets(facetConfigurationRest, configuration.getSidebarFacets());
        }

        return facetConfigurationRest;
    }

    private void addSidebarFacets(FacetConfigurationRest facetConfigurationRest,
                                  List<DiscoverySearchFilterFacet> sidebarFacets) {
        for (DiscoverySearchFilterFacet discoverySearchFilterFacet : CollectionUtils.emptyIfNull(sidebarFacets)) {

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(discoverySearchFilterFacet.getIndexFieldName());
            facetEntry.setFacetType(discoverySearchFilterFacet.getType());
            facetEntry.setFacetLimit(discoverySearchFilterFacet.getFacetLimit());

            facetConfigurationRest.addSidebarFacet(facetEntry);
        }
    }
}
