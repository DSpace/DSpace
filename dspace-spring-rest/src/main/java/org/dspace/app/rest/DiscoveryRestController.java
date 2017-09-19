package org.dspace.app.rest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.InvalidRequestException;
import org.dspace.app.rest.model.DiscoveryRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.utils.DiscoverQueryBuilder;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
@RestController
@RequestMapping("/api/"+ DiscoveryRest.CATEGORY)
public class DiscoveryRestController extends AbstractDSpaceRestRepository implements InitializingBean {

    private static final Logger log = Logger.getLogger(ScopeResolver.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Autowired
    private DiscoverQueryBuilder queryBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(new Link("/api/"+ DiscoveryRest.CATEGORY, DiscoveryRest.CATEGORY)));
    }


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    public void getSearchConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                       @RequestParam(name = "configuration", required = false) String configurationName) {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving search configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        //TODO Call DiscoveryConfigurationConverter on configuration to convert this API model to the REST model

        //TODO Return REST model
        //TODO set "hasMore" property on facets
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/objects")
    public void getSearchObjects(@RequestParam(name = "query", required = false) String query,
                                 @RequestParam(name = "dsoType", required = false) String dsoType,
                                 @RequestParam(name = "scope", required = false) String dsoScope,
                                 @RequestParam(name = "configuration", required = false) String configurationName,
                                 List<SearchFilter> searchFilters,
                                 Pageable page) {
        if(log.isTraceEnabled()) {
            log.trace("Searching with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", configuration name: " + StringUtils.trimToEmpty(configurationName)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType));
            //TODO add filters and page info
        }

        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        try {
            DiscoverQuery discoverQuery = queryBuilder.buildQuery(context, scopeObject, configuration, query, searchFilters, dsoType, page);
            DiscoverResult searchResult = searchService.search(context, scopeObject, discoverQuery);

        } catch (InvalidRequestException e) {
            log.warn("Received an invalid request", e);
            //TODO TOM handle invalid request
        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            //TODO TOM handle search exception
        }

        //TODO convert search result to DSO list
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets")
    public void getFacetsConfiguration(@RequestParam(name = "scope", required = false) String dsoScope,
                                       @RequestParam(name = "configuration", required = false) String configurationName) {
        if(log.isTraceEnabled()) {
            log.trace("Retrieving facet configuration for scope " + StringUtils.trimToEmpty(dsoScope)
                    + " and configuration name " + StringUtils.trimToEmpty(configurationName));
        }

        //TODO
        Context context = obtainContext();

        DSpaceObject scopeObject = scopeResolver.resolveScope(context, dsoScope);
        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByNameOrDso(configurationName, scopeObject);

        //TODO Call DiscoveryConfigurationConverter on configuration to convert this API model to the REST model

        //TODO Return REST model
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facets/{name}")
    public void getFacetValues(@PathVariable("name") String facetName,
                               @RequestParam(name = "query", required = false) String query,
                               @RequestParam(name = "dsoType", required = false) String dsoType,
                               @RequestParam(name = "scope", required = false) String dsoScope,
                               List<SearchFilter> searchFilters,
                               Pageable page) {
        if(log.isTraceEnabled()) {
            log.trace("Facetting on facet " + facetName + " with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", dsoType: " + StringUtils.trimToEmpty(dsoType)
                    + ", query: " + StringUtils.trimToEmpty(dsoType));
            //TODO add filters and page info
        }

        //TODO
    }
}
