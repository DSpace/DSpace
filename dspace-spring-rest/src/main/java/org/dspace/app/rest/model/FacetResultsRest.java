/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * This class provides a container for the information to be used in the FacetResultsResource
 */
public class FacetResultsRest extends DiscoveryResultsRest {

    @JsonUnwrapped
    private SearchFacetEntryRest facetEntry;

    public void addToFacetResultList(SearchFacetValueRest facetResult){
        facetEntry.addValue(facetResult);
    }

    @JsonIgnore
    public List<SearchFacetValueRest> getFacetResultList(){
        return facetEntry.getValues();
    }

    public SearchFacetEntryRest getFacetEntry() {
        return facetEntry;
    }

    public void setFacetEntry(final SearchFacetEntryRest facetEntry) {
        this.facetEntry = facetEntry;
    }
}
