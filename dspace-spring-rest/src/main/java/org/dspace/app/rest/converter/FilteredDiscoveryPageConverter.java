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

/**
 * This converter takes an EntityType and converts it to a FilteredDiscoveryPageRest object to give a
 * representation about the filter query that has to be used for the given EntityType
 */
@Component
public class FilteredDiscoveryPageConverter implements DSpaceConverter<org.dspace.content.EntityType,
                                                                    FilteredDiscoveryPageRest> {
    @Autowired
    private EntityTypeToFilterQueryService entityTypeToFilterQueryService;

    /**
     * This method converts the EntityType object to a FilteredDiscoveryPageRest object to be passed along
     * to the resource and endpoint so that callers can know what filter query they need to use to
     * filter on a particular, given, EntityType
     * @param obj   The EntityType for which this filterQuery string will be looked up for
     * @return      The filterQuery String for the given EntityType
     */
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
