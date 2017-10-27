package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.data.domain.Pageable;


public class FacetResultsRest extends DiscoveryResultsRest {

    @JsonIgnore
    private LinkedList<SearchFacetValueRest> facetResultList = new LinkedList<>();

    @JsonUnwrapped
    private SearchFacetEntryRest facetEntry;

    @JsonIgnore
    private Pageable page;

    public void addToFacetResultList(SearchFacetValueRest facetResult){
        facetResultList.add(facetResult);
    }

    public List<SearchFacetValueRest> getFacetResultList(){
        return facetResultList;
    }

    public Pageable getPage() {
        return page;
    }

    public void setPage(Pageable page) {
        this.page = page;
    }

    public SearchFacetEntryRest getFacetEntry() {
        return facetEntry;
    }

    public void setFacetEntry(final SearchFacetEntryRest facetEntry) {
        this.facetEntry = facetEntry;
    }
}
