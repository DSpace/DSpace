package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.FacetResultEntryRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class FacetResultEntryResource extends HALResource {

    @JsonUnwrapped
    private final FacetResultEntryRest data;


    public FacetResultEntryResource(FacetResultEntryRest facetResultEntryRest){
        this.data = facetResultEntryRest;
        addLinks();
    }

    private void addLinks() {
        //Create the self link using our Controller
        String baseLink = buildBaseLink();

        Link link = new Link(baseLink, Link.REL_SELF);
        add(link);
    }

    private String buildBaseLink() {

        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);

        //TODO move somewhere else ->
        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
                .getSearchObjects(data.getName(), data.getQuery(), null, data.getScope(), new LinkedList<SearchFilter>(), null))
                .toUriComponentsBuilder();

        return addFilterParams(uriComponentsBuilder).build().toString();
    }
    private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder) {
        if (data.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : data.getAppliedFilters()) {
                //TODO Make sure the filter format is defined in only one place
                uriComponentsBuilder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }
        uriComponentsBuilder.queryParam("f."+data.getFacetName(), data.getName() + ",equals");

        return uriComponentsBuilder;
    }

}
