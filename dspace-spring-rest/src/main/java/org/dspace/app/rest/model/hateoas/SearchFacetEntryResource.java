/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.URLUtils;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetEntryResource extends HALResource {

    @JsonUnwrapped
    private SearchFacetEntryRest facetData;

    @JsonIgnore
    private DiscoveryResultsRest searchData;

    public SearchFacetEntryResource(final SearchFacetEntryRest facetData, final DiscoveryResultsRest searchData) {
        this.facetData = facetData;
        this.searchData = searchData;

        addEmbeds();
    }

    public SearchFacetEntryRest getFacetData() {
        return facetData;
    }

    public DiscoveryResultsRest getSearchData() {
        return searchData;
    }

    private void addEmbeds() {
        List<SearchFacetValueResource> valueResourceList = new LinkedList<>();

        for (SearchFacetValueRest valueRest : CollectionUtils.emptyIfNull(facetData.getValues())) {
            SearchFacetValueResource valueResource = new SearchFacetValueResource(valueRest, facetData, searchData);
            valueResourceList.add(valueResource);
        }

        embedResource("values", valueResourceList);
    }
}
