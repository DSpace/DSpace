/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.data.domain.Pageable;

/**
 * This class provides a container for the information to be used in the FacetResultsResource
 */
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
