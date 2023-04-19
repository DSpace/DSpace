/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.contentreport.Filter;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ContentReportSupportRest;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredCollectionsRest;
import org.dspace.app.rest.model.FilteredItemsQuery;
import org.dspace.app.rest.model.FilteredItemsQueryPredicate;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * or as a multivalued parameter
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of collections with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/filteredcollections")
    public ResponseEntity<RepresentationModel<?>> getFilteredCollections(
            @RequestParam(name = "filters", required = false) List<String> filters,
            HttpServletRequest request, HttpServletResponse response) {
        Context context = ContextUtil.obtainContext(request);
        Map<Filter, Boolean> filtersMap = listToStream(filters)
                .map(Filter::get)
                .filter(f -> f != null)
                .collect(Collectors.toMap(Function.identity(), v -> Boolean.TRUE));
        FilteredCollectionsQuery query = FilteredCollectionsQuery.of(filtersMap);
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
     * GET-based endpoint for the Filtered Items contents report.
     * All parameters received as comma-separated lists can also be repeated
     * instead (e.g., filters=a&filters=b&...).
     * This method also serves as a feed for the HAL Browser infrastructure.
     * @param collections comma-separated list UUIDs of collections to include in the report
     * @param predicates predicates to filter the requested items.
     * A given predicate has the form
     * field:operator:value (if value is required by the operator), or
     * field:operator (if no value is required by the operator).
     * The colon is used here as a separator to avoid conflicts with the
     * comma, which is already used by Spring as a multi-value separator.
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
            HttpServletRequest request, HttpServletResponse response, Pageable pageable) {
        Context context = ContextUtil.obtainContext(request);
        List<String> collUuids = Optional.ofNullable(collections).orElseGet(() -> List.of());
        List<FilteredItemsQueryPredicate> preds = listToStream(predicates)
                .map(FilteredItemsQueryPredicate::of)
                .collect(Collectors.toList());
        int pgLimit = parseInt(pageLimit, 10);
        int pgNumber = parseInt(pageNumber, 0);
        Pageable myPageable = pageable;
        if (pageable == null || pageable.getPageNumber() != pgNumber || pageable.getPageSize() != pgLimit) {
            Sort sort = Optional.ofNullable(pageable).map(Pageable::getSort).orElse(Sort.unsorted());
            myPageable = PageRequest.of(pgNumber, pgLimit, sort);
        }
        Map<Filter, Boolean> filtersMap = listToStream(filters)
                .map(Filter::get)
                .filter(f -> f != null)
                .collect(Collectors.toMap(Function.identity(), v -> Boolean.TRUE));
        List<String> addFields = Optional.ofNullable(additionalFields).orElseGet(() -> List.of());
        FilteredItemsQuery query = FilteredItemsQuery.of(collUuids, preds, pgLimit, filtersMap, addFields);

        return filteredItemsReport(context, query, myPageable);
    }

    private static Stream<String> listToStream(Collection<String> array) {
        return Optional.ofNullable(array)
                .stream()
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank);
    }

    private static int parseInt(String value, int defaultValue) {
        return Optional.ofNullable(value)
                .stream()
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * POST-based endpoint for the Filtered Items contents report.
     * @param query structured query parameters
     * @param pageable paging parameters
     * @param request HTTP request
     * @param response HTTP response
     * @return the list of items with their respective statistics
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/filtereditems")
    public ResponseEntity<RepresentationModel<?>> postFilteredItems(
            @RequestBody FilteredItemsQuery query, Pageable pageable,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        return filteredItemsReport(context, query, pageable);
    }

    private ResponseEntity<RepresentationModel<?>> filteredItemsReport(Context context,
            FilteredItemsQuery query, Pageable pageable) {
        FilteredItemsRest report = contentReportRestRepository
                .findFilteredItems(context, query, pageable);
        FilteredItemsResource result = converter.toResource(report);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);
    }

}
