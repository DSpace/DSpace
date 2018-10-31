/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;

/**
 * This class' purpose is to create a container for the information, links and embeds for the facet values on various
 * endpoints
 */
public class SearchFacetValueResource extends HALResource<SearchFacetValueRest> {

    @JsonIgnore
    private SearchFacetEntryRest facetData;

    @JsonIgnore
    private DiscoveryResultsRest searchData;

    public SearchFacetValueResource(final SearchFacetValueRest data, final SearchFacetEntryRest facetData,
                                    final DiscoveryResultsRest searchData) {
        super(data);
        this.facetData = facetData;
        this.searchData = searchData;
    }

    public SearchFacetEntryRest getFacetData() {
        return facetData;
    }

    public DiscoveryResultsRest getSearchData() {
        return searchData;
    }

    @JsonIgnore
    public SearchFacetValueRest getValueData() {
        return getContent();
    }
}
