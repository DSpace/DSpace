/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetEntryConverter
    implements DSpaceConverter<DiscoverySearchFilterFacet, SearchFacetEntryRest> {
    private static final Logger log = LogManager.getLogger(DiscoverFacetEntryConverter.class);

    @Autowired
    private SearchService searchService;

    @Override
    public SearchFacetEntryRest convert(DiscoverySearchFilterFacet discoverySearchFilterFacet, Projection projection) {
        SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(discoverySearchFilterFacet.getIndexFieldName());
        facetEntry.setProjection(projection);
        facetEntry.setFacetType(discoverySearchFilterFacet.getType());
        facetEntry.setFacetLimit(discoverySearchFilterFacet.getFacetLimit());
        facetEntry.setOpenByDefault(discoverySearchFilterFacet.isOpenByDefault());
        facetEntry.setExposeMinMax(discoverySearchFilterFacet.exposeMinAndMaxValue());
        if (discoverySearchFilterFacet.exposeMinAndMaxValue()) {
            Context context = ContextUtil.obtainCurrentRequestContext();
            handleExposeMinMaxValues(context, discoverySearchFilterFacet, facetEntry);
        }
        return facetEntry;
    }

    private void handleExposeMinMaxValues(Context context, DiscoverySearchFilterFacet field,
                                          SearchFacetEntryRest facetEntry) {
        try {
            String minValue = searchService.calculateExtremeValue(context, field.getIndexFieldName() + "_min",
                field.getIndexFieldName() + "_min_sort", DiscoverQuery.SORT_ORDER.asc);
            String maxValue = searchService.calculateExtremeValue(context, field.getIndexFieldName() + "_max",
                field.getIndexFieldName() + "_max_sort", DiscoverQuery.SORT_ORDER.desc);

            if (StringUtils.isNotBlank(minValue) && StringUtils.isNotBlank(maxValue)) {
                facetEntry.setMinValue(minValue);
                facetEntry.setMaxValue(maxValue);
            }
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Class<DiscoverySearchFilterFacet> getModelClass() {
        return DiscoverySearchFilterFacet.class;
    }
}
