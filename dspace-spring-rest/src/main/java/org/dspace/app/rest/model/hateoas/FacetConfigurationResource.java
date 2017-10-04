/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.FacetConfigurationRest;

public class FacetConfigurationResource extends HALResource{
    @JsonUnwrapped
    private final FacetConfigurationRest data;


    public FacetConfigurationResource(FacetConfigurationRest facetConfigurationRest){
        this.data = facetConfigurationRest;
    }
    public FacetConfigurationRest getData(){
        return data;
    }
}
