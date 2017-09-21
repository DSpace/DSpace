package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * TODO TOM UNIT TEST
 */
@RelNameDSpaceResource(SearchResultsRest.NAME)
public class SearchResultsResource extends HALResource {

    @JsonUnwrapped
    private final SearchResultsRest data;

    public SearchResultsResource(final SearchResultsRest data, final Utils utils) {
        this.data = data;


        addLinks(data);

        addEmbeds(data, utils);

    }

    private void addEmbeds(final SearchResultsRest data, final Utils utils) {
        List<SearchResultEntryResource> entryResources = new LinkedList<>();
        for (SearchResultEntryRest searchResultEntry : data.getSearchResults()) {
            entryResources.add(new SearchResultEntryResource(searchResultEntry, utils));
        }

        embedResource("searchResults", new Resources<>(entryResources));

        //TODO ADD FACETS
    }

    private void addLinks(final SearchResultsRest data) {
        //Create the self link using our Controller
        String baseLink = buildBaseLink(data);

        Link link = new Link(baseLink, Link.REL_SELF);
        add(link);
    }

    private String buildBaseLink(final SearchResultsRest data) {
        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);
        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
                .getSearchObjects(data.getQuery(), data.getType(), data.getScope(), data.getConfigurationName(), null, null))
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

        return uriComponentsBuilder;
    }

}
