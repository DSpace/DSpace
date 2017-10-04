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
