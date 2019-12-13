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

import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetValueRest;

/**
 * This class contains links, embeds and information (FacetResultsRest) to be shown in the endpoint /facet/author for
 * example.
 */
public class FacetResultsResource extends HALResource<FacetResultsRest> {

    public FacetResultsResource(FacetResultsRest facetResultsRest) {
        super(facetResultsRest);
        addEmbeds(facetResultsRest);
    }

    public void addEmbeds(FacetResultsRest data) {
        List<SearchFacetValueResource> list = buildEntryList(data);

        embedResource("values", list);
    }

    private static List<SearchFacetValueResource> buildEntryList(FacetResultsRest data) {
        LinkedList<SearchFacetValueResource> list = new LinkedList<>();
        for (SearchFacetValueRest searchFacetValueRest : data.getFacetResultList()) {
            SearchFacetValueResource searchFacetValueResource = new SearchFacetValueResource(searchFacetValueRest,
                                                                                             data.getFacetEntry(),
                                                                                             data);
            list.add(searchFacetValueResource);
        }
        return list;
    }

}
