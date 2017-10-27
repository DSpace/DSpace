package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class FacetResultsResource extends HALResource{

    @JsonUnwrapped
    private final FacetResultsRest data;

    public FacetResultsResource(FacetResultsRest facetResultsRest){
        this.data = facetResultsRest;
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

    public FacetResultsRest getData(){
        return data;
    }

}
