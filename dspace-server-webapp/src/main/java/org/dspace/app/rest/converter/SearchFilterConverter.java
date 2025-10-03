/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;



import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SearchFilterConverter implements DSpaceConverter<DiscoverySearchFilter, SearchFilterRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converter;

    @Override
    public SearchFilterRest convert(DiscoverySearchFilter filter, Projection projection) {
        SearchFilterRest searchFilterRest = new SearchFilterRest();

        searchFilterRest.setFilter(filter.getIndexFieldName());
        searchFilterRest.setType(filter.getType());
        searchFilterRest.setOpenByDefault(filter.isOpenByDefault());
        searchFilterRest.setHasFacets(filter instanceof DiscoverySearchFilterFacet);
        searchFilterRest.setPageSize(filter.getPageSize());
        return searchFilterRest;
    }

    @Override
    public Class<DiscoverySearchFilter> getModelClass() {
        return DiscoverySearchFilter.class;
    }
}
