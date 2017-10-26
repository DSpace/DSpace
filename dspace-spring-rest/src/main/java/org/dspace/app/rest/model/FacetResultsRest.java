package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.domain.Pageable;


public class FacetResultsRest extends DiscoveryResultsRest {

    @JsonIgnore
    private LinkedList<SearchFacetValueRest> facetResultList = new LinkedList<>();

    private String type;
    private String name;
    private boolean hasMore;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public boolean isHasMore(){
        return hasMore;
    }

    public void setHasMore(boolean hasMore){
        this.hasMore = hasMore;
    }

}
