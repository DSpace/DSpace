/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SortOptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SortOptionConverter implements DSpaceConverter<DiscoverySortFieldConfiguration, SortOptionRest> {
    @Override
    public SortOptionRest convert(DiscoverySortFieldConfiguration defaultSortField, Projection projection) {
        SortOptionRest sortOptionRest = new SortOptionRest();

        sortOptionRest.setName(defaultSortField.getMetadataField());
        sortOptionRest.setActualName(defaultSortField.getType());
        sortOptionRest.setSortOrder(defaultSortField.getDefaultSortOrder().name());

        return sortOptionRest;
    }

    @Override
    public Class<DiscoverySortFieldConfiguration> getModelClass() {
        return DiscoverySortFieldConfiguration.class;
    }
}
