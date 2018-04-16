/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.FilteredDiscoveryPageRest;
import org.dspace.content.EntityType;
import org.dspace.content.virtual.EntityTypeToFilterQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FilteredDiscoveryPageConverter extends DSpaceConverter<org.dspace.content.EntityType,
                                                                    FilteredDiscoveryPageRest> {
    @Autowired
    private EntityTypeToFilterQueryService entityTypeToFilterQueryService;

    public FilteredDiscoveryPageRest fromModel(EntityType obj) {
        FilteredDiscoveryPageRest filteredDiscoveryPageRest = new FilteredDiscoveryPageRest();
        filteredDiscoveryPageRest.setId(obj.getLabel());
        filteredDiscoveryPageRest.setLabel(obj.getLabel());
        filteredDiscoveryPageRest.setFilterQueryString(
            entityTypeToFilterQueryService.getFilterQueryForKey(obj.getLabel()));
        return filteredDiscoveryPageRest;
    }

    public EntityType toModel(FilteredDiscoveryPageRest obj) {
        return new EntityType();
    }
}
