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
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.springframework.data.domain.Pageable;

@RelNameDSpaceResource(SearchResultsRest.NAME)
public class FacetsResource extends HALResource<SearchResultsRest> {

    @JsonIgnore
    private List<SearchFacetEntryResource> facetResources = new LinkedList<>();

    public FacetsResource(SearchResultsRest searchResultsRest, Pageable page) {
        super(searchResultsRest);

        embedFacetResults(searchResultsRest, page);
    }

    public List<SearchFacetEntryResource> getFacetResources() {
        return facetResources;
    }

    private void embedFacetResults(SearchResultsRest data, Pageable page) {
        int i = 0;
        for (SearchFacetEntryRest searchFacetEntryRest : CollectionUtils.emptyIfNull(data.getFacets())) {
            if (i >= page.getOffset() && i < page.getOffset() + page.getPageSize()) {
                facetResources.add(new SearchFacetEntryResource(searchFacetEntryRest, data));
            }
        }

        embedResource("facets", facetResources);
    }
}
