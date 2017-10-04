/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscoverFacetConfigurationConverter {
    public FacetConfigurationRest convert(DiscoveryConfiguration configuration){
        FacetConfigurationRest facetConfigurationRest = new FacetConfigurationRest();
        if(configuration != null){
            addSidebarFacets(facetConfigurationRest, configuration.getSidebarFacets());
        }
        return facetConfigurationRest;
    }

    private void addSidebarFacets(FacetConfigurationRest facetConfigurationRest, List<DiscoverySearchFilterFacet> sidebarFacets) {
        for(DiscoverySearchFilterFacet discoverySearchFilterFacet : CollectionUtils.emptyIfNull(sidebarFacets)){
            FacetConfigurationRest.SidebarFacet sidebarFacet = new FacetConfigurationRest.SidebarFacet();
            sidebarFacet.setName(discoverySearchFilterFacet.getIndexFieldName());
            sidebarFacet.setType(discoverySearchFilterFacet.getType());
            facetConfigurationRest.addSidebarFacet(sidebarFacet);
        }
    }
}
