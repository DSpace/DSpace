package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.RootRest;
import org.dspace.app.rest.model.SearchConfigurationRest;

/**
 * Created by raf on 26/09/2017.
 */
public class RootResource extends HALResource {

    @JsonUnwrapped
    private final RootRest data;


    public RootResource(RootRest rootRest) {
        this.data = rootRest;
    }
    public RootRest getData(){
        return data;
    }

}
