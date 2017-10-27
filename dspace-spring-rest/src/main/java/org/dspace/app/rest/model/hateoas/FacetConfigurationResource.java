/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;

import java.util.LinkedList;
import java.util.List;

/**
 * This class' purpose is to provide a resource with the information, links and embeds for the /facet endpoint
 */
public class FacetConfigurationResource extends HALResource<FacetConfigurationRest> {

    public FacetConfigurationResource(FacetConfigurationRest facetConfigurationRest){
        super(facetConfigurationRest);
        addEmbeds(facetConfigurationRest);
    }

    public void addEmbeds(FacetConfigurationRest data) {
        List<SearchFacetEntryResource> searchFacetEntryResources = new LinkedList<>();
        List<FacetConfigurationRest.SidebarFacet> facets = data.getSidebarFacets();
        for (FacetConfigurationRest.SidebarFacet field : CollectionUtils.emptyIfNull(facets)) {

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(field.getName());
            facetEntry.setFacetType(field.getType());
            DiscoveryResultsRest discoveryResultsRest = new SearchResultsRest();
            SearchFacetEntryResource searchFacetEntryResource = new SearchFacetEntryResource(facetEntry, discoveryResultsRest );
            searchFacetEntryResources.add(searchFacetEntryResource);
        }
        embedResource("facets", searchFacetEntryResources);
    }

}
