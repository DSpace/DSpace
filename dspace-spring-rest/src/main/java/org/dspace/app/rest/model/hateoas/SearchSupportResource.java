/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.RelNameDSpaceResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 25/09/2017.
 */
@RelNameDSpaceResource(SearchSupportRest.NAME)
public class SearchSupportResource extends HALResource {

    @JsonUnwrapped
    private final SearchSupportRest data;


    public SearchSupportResource(SearchSupportRest searchSupportRest) {
        this.data = searchSupportRest;
    }
    public SearchSupportRest getData(){
        return data;
    }
}