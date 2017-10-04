/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
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

    public SearchResultsResource(final SearchResultsRest data, final Pageable page, final Utils utils) {
        this.data = data;


        addLinks(data);

        addEmbeds(data, page, utils);

    }

    public SearchResultsRest getData(){
        return data;
    }

    private void addEmbeds(final SearchResultsRest data, final Pageable pageable, final Utils utils) {
        embedSearchResults(data, pageable, utils);

        embedFacetResults(data, utils);
    }

    private void embedFacetResults(final SearchResultsRest data, final Utils utils) {
        List<SearchFacetEntryResource> facetResources = new LinkedList<>();
        for (SearchFacetEntryRest searchFacetEntryRest : CollectionUtils.emptyIfNull(data.getFacets())) {
            facetResources.add(new SearchFacetEntryResource(searchFacetEntryRest, data, utils));
        }

        embedResource("facets", facetResources);
    }

    private void embedSearchResults(final SearchResultsRest data, final Pageable pageable, final Utils utils) {
        List<SearchResultEntryResource> entryResources = new LinkedList<>();
        for (SearchResultEntryRest searchResultEntry : CollectionUtils.emptyIfNull(data.getSearchResults())) {
            entryResources.add(new SearchResultEntryResource(searchResultEntry, utils));
        }

        PageImpl<SearchResultEntryResource> page = new PageImpl<SearchResultEntryResource>(entryResources,
                pageable, data.getTotalNumberOfResults());

        embedResource("searchResults", new EmbeddedPage(buildBaseLink(data), page, entryResources));
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
                .getSearchObjects(data.getQuery(), data.getDsoType(), data.getScope(), data.getConfigurationName(), null, null))
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
