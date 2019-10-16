package org.ssu.controller;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.hibernate.type.IntegerType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.BrowseRequestParameters;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.BrowseContext;
import org.ssu.service.CommunityService;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/browse")
public class BrowseController {
    @Resource
    private CommunityService communityService;

    private List<String> createPaginationLinksList(Integer currentPage, Integer resultsPerPage, Integer totalPages, String currentPageURL) {
        List<String> links = new ArrayList<>();
        String linkFormat = "<li><a href=\"%s&offset=%d\" class = \"%s\">%d</a></li>";
        String separator = "<li><span>...</span></li>";

        Predicate<Integer> needToDisplayLinkToFirstPage = (current) -> current - 2 > 1;
        Predicate<Integer> needToDisplayGapAfterFirstPage = (current) -> current - 2 > 2;
        BiPredicate<Integer, Integer> needToDisplayGapBeforeLastPage = (current, total) -> current + 2 < total - 1;
        BiPredicate<Integer, Integer> needToDisplayLinkToLastPage = (current, total) -> current + 2 < total;
        BiFunction<Integer, String, String> createLink = (pageNumber, style) -> String.format(linkFormat, currentPageURL, (pageNumber - 1) * resultsPerPage, style, pageNumber);

        if(needToDisplayLinkToFirstPage.test(currentPage)) links.add(createLink.apply(1, ""));
        if(needToDisplayGapAfterFirstPage.test(currentPage)) links.add(separator);

        for(int cur = Math.max(1, currentPage - 2); cur <= Math.min(totalPages, currentPage + 2); cur++) {
            links.add(createLink.apply(cur, (cur == currentPage) ? "current" : ""));
        }

        if(needToDisplayGapBeforeLastPage.test(currentPage, totalPages)) links.add(separator);
        if(needToDisplayLinkToLastPage.test(currentPage, totalPages)) links.add(createLink.apply(totalPages, ""));
        return links;
    }

    private Integer getResultsPerPage(Integer resultsPerPageValueFromRequest) {
        return Optional.ofNullable(resultsPerPageValueFromRequest)
                .orElse(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.collectionhome.perpage", 20));
    }

    private ModelAndView fillModelWithData(ModelAndView model, List<ItemResponse> items, BrowseInfo browseInfo, HttpServletRequest request, BrowseRequestParameters requestParameters) throws SortException {
        String currentPageURL = (request.getRequestURL().toString() + "?" + request.getQueryString())
                .replaceAll("[?&]offset=\\d+", "")
//                .replaceAll("[?&]starts_with=[^&]*", "")
                .replaceAll("[?&]year=\\d+", "");
        int currentPage = browseInfo.getOffset() / requestParameters.getItemsPerPage() + 1;
        int totalPages = (int) Math.ceil(Double.valueOf(browseInfo.getTotal()) / requestParameters.getItemsPerPage());

        model.addObject("items", items);
        model.addObject("type", request.getParameter("type"));
        model.addObject("startIndex", browseInfo.getStart());
        model.addObject("finishIndex", browseInfo.getFinish());
        model.addObject("totalItems", browseInfo.getTotal());
        model.addObject("sortedBy", browseInfo.getSortOption());
        model.addObject("sortOrder", requestParameters.getSortOrder());
        model.addObject("rpp", requestParameters.getItemsPerPage());
        model.addObject("selectedYear", requestParameters.getStartsWith().map(String::valueOf).orElse(""));
        model.addObject("sortOptions", SortOption.getSortOptions().stream().filter(SortOption::isVisible).collect(Collectors.toSet()));
        model.addObject("prevPageUrl", String.format("%s&offset=%d", currentPageURL, (currentPage - 2) * requestParameters.getItemsPerPage()));
        model.addObject("prevPageDisabled", browseInfo.hasPrevPage()? "" : "disabled");
        model.addObject("nextPageUrl", String.format("%s&offset=%d", currentPageURL, currentPage * requestParameters.getItemsPerPage()));
        model.addObject("nextPageDisabled", browseInfo.hasNextPage() ? "" : "disabled");
        model.addObject("links", createPaginationLinksList(currentPage, requestParameters.getItemsPerPage(), totalPages, currentPageURL));
        model.addObject("isExtended", requestParameters.getExtendedTable());
        model.setViewName("browse");
        return model;
    }

    @RequestMapping("")
    public ModelAndView getItemsByAuthor(ModelAndView model, HttpServletRequest request, HttpServletResponse response,
                                         @RequestParam(value="type", required = false, defaultValue = "") String type,
                                        @RequestParam(value = "sort_by", defaultValue = "1", required = false) Integer sortBy,
                                        @RequestParam(value="order", defaultValue = "ASC", required = false) String sortOrder,
                                        @RequestParam(value="starts_with", required = false) String startsWith,
                                         @RequestParam(value="year", required = false) String yearParameter,
                                        @RequestParam(value="page", required = false, defaultValue = "1") Integer page,
                                        @RequestParam(value="value", required = false, defaultValue = "") String value,
                                        @RequestParam(value = "rpp", required = false) Integer perPage) throws SQLException, BrowseException, SortException, ServletException, IOException, AuthorizeException {

        Context dspaceContext = UIUtil.obtainContext(request);
        BrowseRequestParameters.Builder requestParameters = new BrowseRequestParameters.Builder()
                .withSortBy(sortBy)
                .withSortOrder(sortOrder)
                .withStartsWith(Optional.ofNullable(startsWith))
                .withPage(page)
                .withItemsPerPage(getResultsPerPage(perPage));

        BrowseInfo browseInfo = new BrowseContext().getBrowseInfo(dspaceContext, request, response);
        List<ItemResponse> items;
        if(("author".equals(type) || "subject".equals(type)) && (value == null || value.isEmpty())) {
             items = communityService.getShortList(dspaceContext, browseInfo);
            requestParameters.withIsExtendedTable(false);
        } else {
            items = communityService.getItems(dspaceContext, browseInfo);
            requestParameters.withIsExtendedTable(true);
        }

        fillModelWithData(model, items, browseInfo, request, requestParameters.build());

        return model;
    }

}
