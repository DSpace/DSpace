/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.QueryParam;

import org.dspace.app.rest.contentreport.Filter;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ContentReportSupportRest;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredCollectionsRest;
import org.dspace.app.rest.model.FilteredItemsQuery;
import org.dspace.app.rest.model.FilteredItemsRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.ContentReportSupportResource;
import org.dspace.app.rest.model.hateoas.FilteredCollectionsResource;
import org.dspace.app.rest.model.hateoas.FilteredItemsResource;
import org.dspace.app.rest.repository.ContentReportRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller receives and dispatches requests related to the
 * contents reports ported from DSpace 6.x (Filtered Collections
 * and Filtered Items).
 * @author Jean-François Morin (Université Laval)
 */
@RestController
@RequestMapping("/api/" + RestAddressableModel.CONTENT_REPORT)
public class ContentReportRestController implements InitializingBean {

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ConverterService converter;

    @Autowired
    private ContentReportRestRepository contentReportRestRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, List.of(Link.of("/api/" + RestModel.CONTENT_REPORT, RestModel.CONTENT_REPORT)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public ContentReportSupportResource getContentReportSupport() {
        ContentReportSupportRest contentReportSupportRest = contentReportRestRepository.getContentReportSupport();
        return converter.toResource(contentReportSupportRest);
    }

    /**
     * GET-based endpoint for the Filtered Collections contents report.
     * This method also serves as a feed for the HAL Browser infrastructure.
     * @param filters querying filters received as a comma-separated string
     * (the separator can actually be anything other than A-Z, a-z, or _.)
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of collections with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/filteredcollections")
    public ResponseEntity<RepresentationModel<?>> getFilteredCollections(@QueryParam("filters") String filters,
            HttpServletRequest request, HttpServletResponse response) {
        Context context = ContextUtil.obtainContext(request);
        FilteredCollectionsQuery query = new FilteredCollectionsQuery();
        query.setFiltersFromCollection(Filter.getFilters(filters));
        return filteredCollectionsReport(context, query);
    }

    /**
     * POST-based endpoint for the Filtered Collections contents report.
     * @param query structured query parameters
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of collections with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/filteredcollections")
    public ResponseEntity<RepresentationModel<?>> postFilteredCollections(
            @RequestBody FilteredCollectionsQuery query,
            HttpServletRequest request, HttpServletResponse response) {
        Context context = ContextUtil.obtainContext(request);
        return filteredCollectionsReport(context, query);
    }

    private ResponseEntity<RepresentationModel<?>> filteredCollectionsReport(Context context,
            FilteredCollectionsQuery query) {
        FilteredCollectionsRest report = contentReportRestRepository
                .findFilteredCollections(context, query);
        FilteredCollectionsResource result = converter.toResource(report);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);
    }

    /**
     * POST-based endpoint for the Filtered Items contents report.
     * @param query structured query parameters
     * @param pageable paging parameters
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of collections with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/filtereditems")
    public ResponseEntity<RepresentationModel<?>> postFilteredItems(
            @RequestBody FilteredItemsQuery query, Pageable pageable,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        FilteredItemsRest report = contentReportRestRepository
                .findFilteredItems(context, query, pageable);
        FilteredItemsResource result = converter.toResource(report);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);
    }

    /**
     * Dummy GET-based method for the /filtereditems endpoint to feed the HAL browser infrastructure.
     * @return nothing, an exception is thrown instead.
     * @throws a RepositoryMethodNotImplementedException instance
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/filtereditems")
    public ResponseEntity<RepresentationModel<?>> getFilteredItems() {
        throw new RepositoryMethodNotImplementedException("Structured parameters required; Method not allowed!", "");
    }

}
