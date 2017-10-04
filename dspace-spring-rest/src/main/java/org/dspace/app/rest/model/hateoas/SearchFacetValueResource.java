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
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetValueResource extends HALResource {

    @JsonUnwrapped
    private SearchFacetValueRest data;

    @JsonIgnore
    private SearchFacetEntryRest facetData;

    @JsonIgnore
    private SearchResultsRest searchData;

    public SearchFacetValueResource(final SearchFacetValueRest data, final SearchFacetEntryRest facetData, final SearchResultsRest searchData, final Utils utils) {
        this.data = data;
        this.facetData = facetData;
        this.searchData = searchData;

        addLinks();
    }

    private void addLinks() {
        //Create the self link using our Controller
        UriComponentsBuilder baseLink = buildBaseLink();

        //add search filter for current facet value
        addFilterForFacetValue(baseLink);

        Link link = new Link(baseLink.build().toString(), "narrow");
        add(link);
    }

    private void addFilterForFacetValue(final UriComponentsBuilder baseLink) {
        baseLink.queryParam("f." + facetData.getName(), data.getFilterValue() + "," + data.getFilterType());
    }

    private UriComponentsBuilder buildBaseLink() {

        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);

        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
                .getSearchObjects(searchData.getQuery(), searchData.getDsoType(), searchData.getScope(), searchData.getConfigurationName(), null, null))
                .toUriComponentsBuilder();

        return addFilterParams(uriComponentsBuilder);
    }


    private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder) {
        if (searchData.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : searchData.getAppliedFilters()) {
                //TODO Make sure the filter format is defined in only one place
                uriComponentsBuilder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }

        return uriComponentsBuilder;
    }
}
