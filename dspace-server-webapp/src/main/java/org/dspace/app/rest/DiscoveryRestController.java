/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for the api/discover endpoint
 */
@RestController
@RequestMapping("/api/" + SearchResultsRest.CATEGORY)
public class DiscoveryRestController implements InitializingBean {

    private static final Logger log = LogManager.getLogger();

    private static final String SOLR_PARSE_ERROR_CLASS = "org.apache.solr.search.SyntaxError";

    @Autowired
    protected Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private DiscoveryRestRepository discoveryRestRepository;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private ConverterService converter;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(Link.of("/api/" + SearchResultsRest.CATEGORY, SearchResultsRest.CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/objects")
    public SearchResultsResource getSearchObjects(@RequestParam(name = "query", required = false) String query,
                                                  @RequestParam(name = "dsoType", required = false)
                                                          List<String> dsoTypes,
                                                  @RequestParam(name = "scope", required = false) String dsoScope,
                                                  @RequestParam(name = "configuration", required = false) String
                                                      configuration,
                                                  List<SearchFilter> searchFilters,
                                                  Pageable page) throws Exception {

        dsoTypes = emptyIfNull(dsoTypes);

        if (log.isTraceEnabled()) {
            log.trace("Searching with scope: " + StringUtils.trimToEmpty(dsoScope)
                    + ", configuration name: " + StringUtils.trimToEmpty(configuration)
                    + ", dsoTypes: " + String.join(", ", dsoTypes)
                    + ", query: " + StringUtils.trimToEmpty(query)
                    + ", filters: " + Objects.toString(searchFilters)
                    + ", page: " + Objects.toString(page));
        }

        //Get the Search results in JSON format
        try {
            SearchResultsRest searchResultsRest = discoveryRestRepository.getSearchObjects(query, dsoTypes, dsoScope,
                configuration, searchFilters, page, utils.obtainProjection());

            //Convert the Search JSON results to paginated HAL resources
            SearchResultsResource searchResultsResource = new SearchResultsResource(searchResultsRest, utils);
            halLinkService.addLinks(searchResultsResource, page);
            return searchResultsResource;
        } catch (IllegalArgumentException e) {
            boolean isParsingException = e.getMessage().contains(SOLR_PARSE_ERROR_CLASS);
            if (isParsingException) {
                throw new UnprocessableEntityException(e.getMessage());
            } else {
                throw e;
            }
        }
    }

}
