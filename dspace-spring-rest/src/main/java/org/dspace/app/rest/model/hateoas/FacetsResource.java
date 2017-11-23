/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

import java.util.LinkedList;
import java.util.List;

@RelNameDSpaceResource(SearchResultsRest.NAME)
public class FacetsResource extends HALResource<SearchResultsRest>{

    public FacetsResource(SearchResultsRest searchResultsRest) {
        super(searchResultsRest);

        embedFacetResults(searchResultsRest);

    }

    private void embedFacetResults(final SearchResultsRest data) {
        List<SearchFacetEntryResource> facetResources = new LinkedList<>();
        for (SearchFacetEntryRest searchFacetEntryRest : CollectionUtils.emptyIfNull(data.getFacets())) {
            facetResources.add(new SearchFacetEntryResource(searchFacetEntryRest, data));
        }

        embedResource("facets", facetResources);
    }
}
