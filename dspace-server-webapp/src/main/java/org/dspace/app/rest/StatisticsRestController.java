/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.model.hateoas.SearchEventResource;
import org.dspace.app.rest.model.hateoas.StatisticsSupportResource;
import org.dspace.app.rest.model.hateoas.ViewEventResource;
import org.dspace.app.rest.repository.SearchEventRestRepository;
import org.dspace.app.rest.repository.StatisticsRestRepository;
import org.dspace.app.rest.repository.UsageReportRestRepository;
import org.dspace.app.rest.repository.ViewEventRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + RestAddressableModel.STATISTICS)
public class StatisticsRestController implements InitializingBean {

    @Autowired
    private Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private ConverterService converter;

    @Autowired
    private StatisticsRestRepository statisticsRestRepository;

    @Autowired
    private ViewEventRestRepository viewEventRestRepository;

    @Autowired
    private SearchEventRestRepository searchEventRestRepository;

    @Autowired
    private UsageReportRestRepository usageReportRestRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays
                .asList(new Link("/api/" + RestAddressableModel.STATISTICS, RestAddressableModel.STATISTICS)));
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

    @RequestMapping(method = RequestMethod.GET, value = "/usagereports")
    public PagedModel<SearchEventResource> getUsageReports() {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!",
            "getUsageReports");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/usagereports/{uuid_id}")
    @PreAuthorize("hasPermission(#uuidObjectReportId, 'usagereport', 'READ')")
    public UsageReportRest getUsageReport(@PathVariable(name = "uuid_id") String uuidObjectReportId,
                                          HttpServletRequest request)
        throws ParseException, SolrServerException, IOException {
        if (StringUtils.countMatches(uuidObjectReportId, "_") != 1) {
            throw new IllegalArgumentException("Must end in objectUUID_reportId, example: " +
                                               "1911e8a4-6939-490c-b58b-a5d70f8d91fb_TopCountries");
        }
        UUID uuidObject = UUID.fromString(StringUtils.substringBefore(uuidObjectReportId, "_"));
        String reportId = StringUtils.substringAfter(uuidObjectReportId, "_");
        Context context = ContextUtil.obtainContext(request);

        UsageReportRest usageReportRest = usageReportRestRepository.createUsageReport(context, uuidObject, reportId);

        return usageReportRest;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/usagereports/search/object")
    @PreAuthorize("hasPermission(#uri, 'usagereportsearch', 'READ')")
    public Page<UsageReportRest> searchUsageReports(HttpServletRequest request,
                                                    @RequestParam(name = "uri", required = true) String uri,
                                                    Pageable pageable)
        throws SQLException, IOException, ParseException, SolrServerException {
        UUID uuid = UUID.fromString(StringUtils.substringAfterLast(uri, "/"));
        Context context = ContextUtil.obtainContext(request);
        List<UsageReportRest> usageReportsOfItem = usageReportRestRepository.getUsageReportsOfDSO(context, uuid);
        return converter.toRestPage(usageReportsOfItem, pageable, utils.obtainProjection());
    }

}
