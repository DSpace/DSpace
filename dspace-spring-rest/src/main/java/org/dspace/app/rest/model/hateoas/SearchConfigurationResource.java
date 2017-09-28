/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 22/09/2017.
 */
@RelNameDSpaceResource(SearchConfigurationRest.NAME)
public class SearchConfigurationResource extends HALResource{

    @JsonUnwrapped
    private final SearchConfigurationRest data;


    public SearchConfigurationResource(SearchConfigurationRest searchConfigurationRest){
        this.data = searchConfigurationRest;
    }
    public SearchConfigurationRest getData(){
        return data;
    }
}
