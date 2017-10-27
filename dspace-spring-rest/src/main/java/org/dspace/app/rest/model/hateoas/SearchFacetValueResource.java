/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.URLUtils;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetValueResource extends HALResource {

    @JsonUnwrapped
    private SearchFacetValueRest valueData;

    @JsonIgnore
    private SearchFacetEntryRest facetData;

    @JsonIgnore
    private DiscoveryResultsRest searchData;

    public SearchFacetValueResource(final SearchFacetValueRest data, final SearchFacetEntryRest facetData, final DiscoveryResultsRest searchData) {
        this.valueData = data;
        this.facetData = facetData;
        this.searchData = searchData;
    }

    public SearchFacetEntryRest getFacetData() {
        return facetData;
    }

    public DiscoveryResultsRest getSearchData() {
        return searchData;
    }

    public SearchFacetValueRest getValueData() {
        return valueData;
    }
}
