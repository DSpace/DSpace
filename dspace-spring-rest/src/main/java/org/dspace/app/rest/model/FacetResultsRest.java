package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.discovery.DiscoverResult;
import org.springframework.data.domain.Pageable;

import java.util.LinkedList;
import java.util.List;


public class FacetResultsRest extends ResultsRest {


    @JsonIgnore
    private LinkedList<SearchFacetValueRest> facetResultList = new LinkedList<>();

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

    public String type;

    public String getType(){
        return type;
    }
    public void setType(String type){
        this.type = type;
    }
    private Pageable page;
    private String name;
    private boolean hasMore;

    public boolean isHasMore(){
        return hasMore;
    }
    public void setHasMore(boolean hasMore){
        this.hasMore = hasMore;
    }


}
