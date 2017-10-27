package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.discovery.DiscoverResult; /**
 * TODO TOM UNIT TEST
 */
public class DiscoverFacetValueConverter {

    public SearchFacetValueRest convert(final DiscoverResult.FacetResult value) {
        SearchFacetValueRest valueRest = new SearchFacetValueRest();
        valueRest.setLabel(value.getDisplayedValue());
        valueRest.setFilterValue(value.getAsFilterQuery());
        valueRest.setFilterType(value.getFilterType());
        valueRest.setAuthorityKey(value.getAuthorityKey());
        valueRest.setSortValue(value.getSortValue());
        valueRest.setCount(value.getCount());

        return valueRest;
    }
}
