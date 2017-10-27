package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetValueRest;

public class FacetResultsResource extends HALResource<FacetResultsRest>{

    public FacetResultsResource(FacetResultsRest facetResultsRest){
        super(facetResultsRest);
        addEmbeds(facetResultsRest);
    }

    public void addEmbeds(FacetResultsRest data) {
        List<SearchFacetValueResource> list = buildEntryList(data);

        embedResource("values", list);
    }

    private static List<SearchFacetValueResource> buildEntryList(FacetResultsRest data) {
        LinkedList<SearchFacetValueResource> list = new LinkedList<>();
        for(SearchFacetValueRest searchFacetValueRest : data.getFacetResultList()){
            SearchFacetValueResource searchFacetValueResource = new SearchFacetValueResource(searchFacetValueRest, null, data);
            list.add(searchFacetValueResource);
        }
        return list;
    }

}
