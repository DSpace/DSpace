/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * TODO TOM UNIT TEST
 */
@RestController
@RequestMapping("/api/"+ SearchResultsRest.CATEGORY)
public class DiscoveryRestController implements InitializingBean {

    private static final Logger log = Logger.getLogger(ScopeResolver.class);

    @Autowired
    protected Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private DiscoveryRestRepository discoveryRestRepository;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private HalLinkService halLinkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(new Link("/api/"+ SearchResultsRest.CATEGORY, SearchResultsRest.CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public SearchSupportResource getSearchSupport(@RequestParam(name = "scope", required = false) String dsoScope,
                                                  @RequestParam(name = "configuration", required = false) String configurationName){

        SearchSupportRest searchSupportRest = discoveryRestRepository.getSearchSupport();
        SearchSupportResource searchSupportResource = new SearchSupportResource(searchSupportRest);
        halLinkService.addLinks(searchSupportResource);
        return searchSupportResource;
    }
    @RequestMapping(method = RequestMethod.GET, value = "/search")
    public SearchConfigurationResource getSearchConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                                              @RequestParam(name = "configuration", required = false) String configurationName) {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving search configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        SearchConfigurationRest searchConfigurationRest = discoveryRestRepository.getSearchConfiguration(dsoScope, configurationName);

        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(searchConfigurationRest);
        halLinkService.addLinks(searchConfigurationResource);
        return searchConfigurationResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/objects")
    public SearchResultsResource getSearchObjects(@RequestParam(name = "query", required = false) String query,
                                                   @RequestParam(name = "dsoType", required = false) String dsoType,
                                                   @RequestParam(name = "scope", required = false) String dsoScope,
                                                   @RequestParam(name = "configuration", required = false) String configurationName,
                                                   List<SearchFilter> searchFilters,
                                                   Pageable page) {
        if(log.isTraceEnabled()) {
            log.trace("Searching with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", configuration name: " + StringUtils.trimToEmpty(configurationName)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType)
                    + ", filters: " + Objects.toString(searchFilters)
                    + ", page: " + Objects.toString(page));
        }

        //Get the Search results in JSON format
        SearchResultsRest searchResultsRest = discoveryRestRepository.getSearchObjects(query, dsoType, dsoScope, configurationName, searchFilters, page);

        //Convert the Search JSON results to paginated HAL resources
        return new SearchResultsResource(searchResultsRest, page, utils);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets")
    public void getFacetsConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                       @RequestParam(name = "configuration", required = false) String configurationName) {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving facet configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        discoveryRestRepository.getFacetsConfiguration(dsoScope, configurationName);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets/{name}")
    public ResourceSupport getFacetValues(@PathVariable("name") String facetName,
                                          @RequestParam(name = "query", required = false) String query,
                                          @RequestParam(name = "dsoType", required = false) String dsoType,
                                          @RequestParam(name = "scope", required = false) String dsoScope,
                                          List<SearchFilter> searchFilters,
                                          Pageable page) {
        if(log.isTraceEnabled()) {
            log.trace("Facetting on facet " + facetName + " with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType)
                    + ", filters: " + Objects.toString(searchFilters)
                    + ", page: " + Objects.toString(page));
        }

        //TODO

        return null;
    }

}
