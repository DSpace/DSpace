/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.DiscoverResult;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to convert a DiscoverResult.FacetResult object into a SearchFacetValueRest object
 */
@Component
public class DiscoverFacetValueConverter {

    public SearchFacetValueRest convert(final DiscoverResult.FacetResult value, final Projection projection) {
        SearchFacetValueRest valueRest = new SearchFacetValueRest();
        valueRest.setProjection(projection);
        valueRest.setLabel(value.getDisplayedValue());
        valueRest.setFilterValue(value.getAsFilterQuery());
        valueRest.setFilterType(value.getFilterType());
        valueRest.setAuthorityKey(value.getAuthorityKey());
        valueRest.setSortValue(value.getSortValue());
        valueRest.setCount(value.getCount());

        return valueRest;
    }
}
