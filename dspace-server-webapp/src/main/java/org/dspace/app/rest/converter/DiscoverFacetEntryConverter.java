/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetEntryConverter
    implements DSpaceConverter<DiscoverySearchFilterFacet, SearchFacetEntryRest> {

    @Override
    public SearchFacetEntryRest convert(DiscoverySearchFilterFacet discoverySearchFilterFacet, Projection projection) {
        SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(discoverySearchFilterFacet.getIndexFieldName());
        facetEntry.setProjection(projection);
        facetEntry.setFacetType(discoverySearchFilterFacet.getType());
        facetEntry.setFacetLimit(discoverySearchFilterFacet.getFacetLimit());
        facetEntry.setOpenByDefault(discoverySearchFilterFacet.isOpenByDefault());
        return facetEntry;
    }

    @Override
    public Class<DiscoverySearchFilterFacet> getModelClass() {
        return DiscoverySearchFilterFacet.class;
    }
}
