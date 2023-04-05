/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Arrays;
import java.util.UUID;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.hateoas.SearchEventResource;
import org.dspace.app.rest.model.hateoas.StatisticsSupportResource;
import org.dspace.app.rest.model.hateoas.ViewEventResource;
import org.dspace.app.rest.repository.SearchEventRestRepository;
import org.dspace.app.rest.repository.StatisticsRestRepository;
import org.dspace.app.rest.repository.ViewEventRestRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + RestAddressableModel.STATISTICS)
public class StatisticsRestController implements InitializingBean {

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ConverterService converter;

    @Autowired
    private StatisticsRestRepository statisticsRestRepository;

    @Autowired
    private ViewEventRestRepository viewEventRestRepository;

    @Autowired
    private SearchEventRestRepository searchEventRestRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays
                .asList(Link.of("/api/" + RestAddressableModel.STATISTICS, RestAddressableModel.STATISTICS)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public StatisticsSupportResource getStatisticsSupport() throws Exception {
        StatisticsSupportRest statisticsSupportRest = statisticsRestRepository.getStatisticsSupport();
        return converter.toResource(statisticsSupportRest);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/viewevents/{uuid}")
    public PagedModel<ViewEventResource> getViewEvent(@PathVariable(name = "uuid") UUID uuid) throws Exception {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/searchevents/{uuid}")
    public PagedModel<SearchEventResource> getSearchEvent(@PathVariable(name = "uuid") UUID uuid) throws Exception {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/viewevents")
    public PagedModel<ViewEventResource> getViewEvents() throws Exception {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/searchevents")
    public PagedModel<SearchEventResource> getSearchEvents() throws Exception {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/viewevents")
    public ResponseEntity<RepresentationModel<?>> postViewEvent() throws Exception {
        ViewEventResource result = converter.toResource(viewEventRestRepository.createViewEvent());
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/searchevents")
    public ResponseEntity<RepresentationModel<?>> postSearchEvent() throws Exception {
        SearchEventResource result = converter.toResource(searchEventRestRepository.createSearchEvent());
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
    }

}
