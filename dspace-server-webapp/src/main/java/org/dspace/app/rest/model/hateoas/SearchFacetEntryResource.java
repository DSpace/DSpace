/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;

/**
 * This class' purpose is to create a container with the information, links and embeds for the different facets on
 * various endpoints
 */
public class SearchFacetEntryResource extends HALResource<SearchFacetEntryRest> {

    @JsonIgnore
    private DiscoveryResultsRest searchData;

    public SearchFacetEntryResource(final SearchFacetEntryRest facetData, final DiscoveryResultsRest searchData) {
        super(facetData);
        this.searchData = searchData;

        addEmbeds();
    }

    public SearchFacetEntryResource(final SearchFacetEntryRest facetData) {
        this(facetData, null);
    }

    @JsonIgnore
    public SearchFacetEntryRest getFacetData() {
        return getContent();
    }

    public DiscoveryResultsRest getSearchData() {
        return searchData;
    }

    private void addEmbeds() {
        if (searchData != null) {
            List<SearchFacetValueResource> valueResourceList = new LinkedList<>();

            for (SearchFacetValueRest valueRest : CollectionUtils.emptyIfNull(getContent().getValues())) {
                SearchFacetValueResource valueResource = new SearchFacetValueResource(valueRest, getContent(),
                                                                                      searchData);
                valueResourceList.add(valueResource);
            }

            embedResource("values", valueResourceList);
        }
    }
}
