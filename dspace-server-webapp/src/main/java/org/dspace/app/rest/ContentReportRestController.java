/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ContentReportSupportRest;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredCollectionsRest;
import org.dspace.app.rest.model.FilteredItemsQueryPredicate;
import org.dspace.app.rest.model.FilteredItemsQueryRest;
import org.dspace.app.rest.model.FilteredItemsRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.ContentReportSupportResource;
import org.dspace.app.rest.model.hateoas.FilteredCollectionsResource;
import org.dspace.app.rest.model.hateoas.FilteredItemsResource;
import org.dspace.app.rest.repository.ContentReportRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller receives and dispatches requests related to the
 * contents reports ported from DSpace 6.x (Filtered Collections
 * and Filtered Items).
 * @author Jean-François Morin (Université Laval)
 */
@RestController
@RequestMapping("/api/" + RestModel.CONTENT_REPORT)
public class ContentReportRestController implements InitializingBean {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private ContentReportRestRepository contentReportRestRepository;
    @Autowired
    private ContentReportService contentReportService;

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
     * or as a multivalued parameter
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of collections with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/filteredcollections")
    public ResponseEntity<RepresentationModel<?>> getFilteredCollections(
            @RequestParam(name = "filters", required = false) List<String> filters,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (contentReportService.getEnabled()) {
            Context context = ContextUtil.obtainContext(request);
            Set<Filter> filtersSet = listToStream(filters)
                    .map(Filter::get)
                    .filter(f -> f != null)
                    .collect(Collectors.toSet());
            FilteredCollectionsQuery query = FilteredCollectionsQuery.of(filtersSet);
            return filteredCollectionsReport(context, query);
        }

        error404(response);
        return null;
    }


    private ResponseEntity<RepresentationModel<?>> filteredCollectionsReport(Context context,
            FilteredCollectionsQuery query) {
        FilteredCollectionsRest report = contentReportRestRepository
                .findFilteredCollections(context, query);
        FilteredCollectionsResource result = converter.toResource(report);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);
    }

    /**
     * Endpoint for the Filtered Items contents report.
     * All parameters received as comma-separated lists can also be repeated
     * instead (e.g., filters=a&filters=b&...).
     * @param collections comma-separated list UUIDs of collections to include in the report
     * @param predicates predicates to filter the requested items.
     * A given predicate has the form
     * field:operator:value (if value is required by the operator), or
     * field:operator (if no value is required by the operator).
     * The colon is used here as a separator to avoid conflicts with the
     * comma, which is already used by Spring as a multi-value separator.
     * Predicates are actually retrieved directly through the request to prevent comma-containing
     * predicate values from being split by the Spring infrastructure.
     * @param pageNumber page number (starting at 0)
     * @param pageLimit maximum number of items per page
     * @param filters querying filters received as a comma-separated string
     * @param additionalFields comma-separated list of extra fields to add to the report
     * @param request HTTP request
     * @param response HTTP response
     * @param pageable paging parameters
     * @return the list of items with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/filtereditems")
    public ResponseEntity<RepresentationModel<?>> getFilteredItems(
            @RequestParam(name = "collections", required = false) List<String> collections,
            @RequestParam(name = "queryPredicates", required = false) List<String> predicates,
            @RequestParam(name = "pageNumber", defaultValue = "0") String pageNumber,
            @RequestParam(name = "pageLimit", defaultValue = "10") String pageLimit,
            @RequestParam(name = "filters", required = false) List<String> filters,
            @RequestParam(name = "additionalFields", required = false) List<String> additionalFields,
            HttpServletRequest request, HttpServletResponse response, Pageable pageable) throws IOException {
        if (contentReportService.getEnabled()) {
            Context context = ContextUtil.obtainContext(request);
            String[] realPredicates = request.getParameterValues("queryPredicates");
            List<String> collUuids = Optional.ofNullable(collections).orElseGet(() -> List.of());
            List<FilteredItemsQueryPredicate> preds = arrayToStream(realPredicates)
                    .map(FilteredItemsQueryPredicate::of)
                    .collect(Collectors.toList());
            int pgLimit = parseInt(pageLimit, 10);
            int pgNumber = parseInt(pageNumber, 0);
            Pageable myPageable = pageable;
            if (pageable == null || pageable.getPageNumber() != pgNumber || pageable.getPageSize() != pgLimit) {
                Sort sort = Optional.ofNullable(pageable).map(Pageable::getSort).orElse(Sort.unsorted());
                myPageable = PageRequest.of(pgNumber, pgLimit, sort);
            }
            Set<Filter> filtersMap = listToStream(filters)
                    .map(Filter::get)
                    .filter(f -> f != null)
                    .collect(Collectors.toSet());
            List<String> addFields = Optional.ofNullable(additionalFields).orElseGet(() -> List.of());
            FilteredItemsQueryRest query = FilteredItemsQueryRest.of(collUuids, preds, pgLimit, filtersMap, addFields);

            return filteredItemsReport(context, query, myPageable);
        }

        error404(response);
        return null;
    }

    private static Stream<String> listToStream(Collection<String> array) {
        return Optional.ofNullable(array)
                .stream()
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank);
    }

    private static Stream<String> arrayToStream(String... array) {
        return Optional.ofNullable(array)
                .stream()
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank);
    }

    private static int parseInt(String value, int defaultValue) {
        return Optional.ofNullable(value)
                .stream()
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElse(defaultValue);
    }

    private ResponseEntity<RepresentationModel<?>> filteredItemsReport(Context context,
            FilteredItemsQueryRest query, Pageable pageable) {
        FilteredItemsRest report = contentReportRestRepository
                .findFilteredItems(context, query, pageable);
        FilteredItemsResource result = converter.toResource(report);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);
    }

    private void error404(HttpServletResponse response) throws IOException {
        log.debug("Content Reports are disabled");
        String err = "Content Reports are disabled";
        response.setStatus(404);
        response.setContentType("text/html");
        response.setContentLength(err.length());
        response.getWriter().write(err);
    }

}
