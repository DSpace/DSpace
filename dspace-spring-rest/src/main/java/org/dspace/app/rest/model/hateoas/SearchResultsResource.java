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

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TODO TOM UNIT TEST
 */
@RelNameDSpaceResource(SearchResultsRest.NAME)
public class SearchResultsResource extends HALResource {

    @JsonUnwrapped
    private final SearchResultsRest data;

    @JsonIgnore
    private List<SearchResultEntryResource> entryResources;

    public SearchResultsResource(final SearchResultsRest data, final Utils utils) {
        this.data = data;

        addEmbeds(data, utils);
    }

    public SearchResultsRest getData(){
        return data;
    }

    public List<SearchResultEntryResource> getEntryResources() {
        return entryResources;
    }

    private void addEmbeds(final SearchResultsRest data, final Utils utils) {
        embedSearchResults(data, utils);

        embedFacetResults(data);
    }

    private void embedFacetResults(final SearchResultsRest data) {
        List<SearchFacetEntryResource> facetResources = new LinkedList<>();
        for (SearchFacetEntryRest searchFacetEntryRest : CollectionUtils.emptyIfNull(data.getFacets())) {
            facetResources.add(new SearchFacetEntryResource(searchFacetEntryRest, data));
        }

        embedResource("facets", facetResources);
    }

    private void embedSearchResults(final SearchResultsRest data, final Utils utils) {
        entryResources = new LinkedList<>();
        for (SearchResultEntryRest searchResultEntry : CollectionUtils.emptyIfNull(data.getSearchResults())) {
            entryResources.add(new SearchResultEntryResource(searchResultEntry, utils));
        }

        embedResource("searchResults", entryResources);
    }

}
