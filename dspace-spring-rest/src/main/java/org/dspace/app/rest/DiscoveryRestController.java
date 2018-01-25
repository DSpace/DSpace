/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.model.hateoas.FacetConfigurationResource;
import org.dspace.app.rest.model.hateoas.FacetResultsResource;
import org.dspace.app.rest.model.hateoas.FacetsResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for the api/discover endpoint
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
    private HalLinkService halLinkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(new Link("/api/"+ SearchResultsRest.CATEGORY, SearchResultsRest.CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public SearchSupportResource getSearchSupport(@RequestParam(name = "scope", required = false) String dsoScope,
                                                  @RequestParam(name = "configuration", required = false) String configurationName) throws Exception {

        SearchSupportRest searchSupportRest = discoveryRestRepository.getSearchSupport();
        SearchSupportResource searchSupportResource = new SearchSupportResource(searchSupportRest);
        halLinkService.addLinks(searchSupportResource);
        return searchSupportResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search")
    public SearchConfigurationResource getSearchConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                                              @RequestParam(name = "configuration", required = false) String configurationName) throws Exception {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving search configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        SearchConfigurationRest searchConfigurationRest = discoveryRestRepository.getSearchConfiguration(dsoScope, configurationName);

        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(searchConfigurationRest);
        halLinkService.addLinks(searchConfigurationResource);
        return searchConfigurationResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/facets")
    public FacetsResource getFacets(@RequestParam(name = "query", required = false) String query,
                                    @RequestParam(name = "dsoType", required = false) String dsoType,
                                    @RequestParam(name = "scope", required = false) String dsoScope,
                                    @RequestParam(name = "configuration", required = false) String configurationName,
                                    List<SearchFilter> searchFilters) throws Exception {

        if(log.isTraceEnabled()) {
            log.trace("Searching with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", configuration name: " + StringUtils.trimToEmpty(configurationName)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType)
                    + ", filters: " + Objects.toString(searchFilters));
        }

        SearchResultsRest searchResultsRest = discoveryRestRepository.getAllFacets(query, dsoType, dsoScope, configurationName, searchFilters);

        FacetsResource facetsResource = new FacetsResource(searchResultsRest);
        halLinkService.addLinks(facetsResource);

        return facetsResource;


    }
    @RequestMapping(method = RequestMethod.GET, value = "/search/objects")
    public SearchResultsResource getSearchObjects(@RequestParam(name = "query", required = false) String query,
                                                   @RequestParam(name = "dsoType", required = false) String dsoType,
                                                   @RequestParam(name = "scope", required = false) String dsoScope,
                                                   @RequestParam(name = "configuration", required = false) String configurationName,
                                                   List<SearchFilter> searchFilters,
                                                   Pageable page) throws Exception {
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
        SearchResultsResource searchResultsResource = new SearchResultsResource(searchResultsRest, utils);
        halLinkService.addLinks(searchResultsResource, page);
        return searchResultsResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets")
    public FacetConfigurationResource getFacetsConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                       @RequestParam(name = "configuration", required = false) String configurationName) throws Exception {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving facet configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        FacetConfigurationRest facetConfigurationRest = discoveryRestRepository.getFacetsConfiguration(dsoScope, configurationName);
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(facetConfigurationRest);

        halLinkService.addLinks(facetConfigurationResource);
        return facetConfigurationResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets/{name}")
    public ResourceSupport getFacetValues(@PathVariable("name") String facetName,
                                                         @RequestParam(name = "query", required = false) String query,
                                                         @RequestParam(name = "dsoType", required = false) String dsoType,
                                                         @RequestParam(name = "scope", required = false) String dsoScope,
                                                         List<SearchFilter> searchFilters,
                                                         Pageable page) throws Exception {
        if(log.isTraceEnabled()) {
            log.trace("Facetting on facet " + facetName + " with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType)
                    + ", filters: " + Objects.toString(searchFilters)
                    + ", page: " + Objects.toString(page));
        }

        FacetResultsRest facetResultsRest = discoveryRestRepository.getFacetObjects(facetName, query, dsoType, dsoScope, searchFilters, page);

        FacetResultsResource facetResultsResource = new FacetResultsResource(facetResultsRest);

        halLinkService.addLinks(facetResultsResource, page);
        return facetResultsResource;
    }

}
