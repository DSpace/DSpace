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
import org.dspace.app.rest.model.*;
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
    private ResultsRest searchData;

    public SearchFacetValueResource(final SearchFacetValueRest data, final SearchFacetEntryRest facetData, final ResultsRest searchData, final Utils utils) {
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

        Link link = new Link(baseLink.build().toString(), "search");
        add(link);
    }

    private void addFilterForFacetValue(final UriComponentsBuilder baseLink) {
        //TODO ugly
        if(facetData!=null){
            baseLink.queryParam("f." + facetData.getName(), data.getFilterValue() + "," + data.getFilterType());
        }
        else{
            baseLink.queryParam("f." + ((FacetResultsRest) searchData).getName(), data.getLabel() + "," + "equals");
        }
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
