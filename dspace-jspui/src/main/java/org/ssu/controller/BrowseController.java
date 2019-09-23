package org.ssu.controller;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.*;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.hibernate.type.IntegerType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.CommunityService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private List<String> createPaginationLinksList(Integer currentPage, Integer totalPages, String currentPageURL) {
        List<String> links = new ArrayList<>();
        String linkFormat = "<li><a href=\"%s&page=%d\" class = \"%s\">%d</a></li>";
        String separator = "<li><span>...</span></li>";

        Predicate<Integer> needToDisplayLinkToFirstPage = (current) -> current - 2 > 1;
        Predicate<Integer> needToDisplayGapAfterFirstPage = (current) -> current - 2 > 2;
        BiPredicate<Integer, Integer> needToDisplayGapBeforeLastPage = (current, total) -> current + 2 < total - 1;
        BiPredicate<Integer, Integer> needToDisplayLinkToLastPage = (current, total) -> current + 2 < total;
        BiFunction<Integer, String, String> createLink = (pageNumber, style) -> String.format(linkFormat, currentPageURL, pageNumber, style, pageNumber);

        if(needToDisplayLinkToFirstPage.test(currentPage)) links.add(createLink.apply(1, ""));
        if(needToDisplayGapAfterFirstPage.test(currentPage)) links.add(separator);

        for(int cur = Math.max(1, currentPage - 2); cur <= Math.min(totalPages, currentPage + 2); cur++) {
            links.add(createLink.apply(cur, (cur == currentPage) ? "current" : ""));
        }

        if(needToDisplayGapBeforeLastPage.test(currentPage, totalPages)) links.add(separator);
        if(needToDisplayLinkToLastPage.test(currentPage, totalPages)) links.add(createLink.apply(totalPages, ""));
        return links;
    }


    @RequestMapping("/dateissued")
    public ModelAndView getItemsByDate(ModelAndView model, HttpServletRequest request, HttpServletResponse response,
                                       @RequestParam(value = "sort_by", defaultValue = "2", required = false) Integer sortBy,
                                       @RequestParam(value="order", defaultValue = "ASC", required = false) String sortOrder,
                                       @RequestParam(value="year", required = false) Integer yearParameter,
                                       @RequestParam(value="page", required = false, defaultValue = "1") Integer page,
                                       @RequestParam(value = "rpp", required = false) String perPage) throws SQLException, BrowseException, SortException {

        Context dspaceContext = UIUtil.obtainContext(request);
        BrowseEngine browseEngine = new BrowseEngine(dspaceContext);
        BrowserScope browserScope = new BrowserScope(dspaceContext);
        BrowseIndex browseIndex = BrowseIndex.getItemBrowseIndex();

        Integer resultsPerPage = Optional.ofNullable(perPage)
                .map(Integer::parseInt)
                .orElse(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.collectionhome.perpage", 20));
        Optional<Integer> year = Optional.ofNullable(yearParameter);
        year.ifPresent(value -> browserScope.setStartsWith(value.toString()));

        browserScope.setSortBy(sortBy);
        browserScope.setOrder(sortOrder);
        browserScope.setResultsPerPage(resultsPerPage);
        browserScope.setBrowseIndex(browseIndex);
        browserScope.setOffset(resultsPerPage * (page - 1));
        BrowseInfo browseInfo = browseEngine.browse(browserScope);

        List<ItemResponse> items = communityService.getItems(dspaceContext, browseInfo);

        String currentPageURL = (request.getRequestURL().toString() + "?" + request.getQueryString())
                .replaceAll("&page=\\d+", "")
                .replaceAll("&year=\\d+", "");
        int currentPage = browseInfo.getOffset() / resultsPerPage + 1;
        int totalPages = (int) Math.ceil(Double.valueOf(browseInfo.getTotal()) / resultsPerPage);

        model.addObject("items", items);
        model.addObject("startIndex", browseInfo.getStart());
        model.addObject("finishIndex", browseInfo.getFinish());
        model.addObject("totalItems", browseInfo.getTotal());
        model.addObject("sortedBy", browseInfo.getSortOption());
        model.addObject("sortOrder", sortOrder);
        model.addObject("rpp", resultsPerPage);
        model.addObject("selectedYear", year.map(String::valueOf).orElse(""));
        model.addObject("sortOptions", SortOption.getSortOptions().stream().filter(SortOption::isVisible).collect(Collectors.toSet()));
        model.addObject("prevPageUrl", String.format("%s&page=%d", currentPageURL, currentPage - 1));
        model.addObject("prevPageDisabled", browseInfo.hasPrevPage()? "" : "disabled");
        model.addObject("nextPageUrl", String.format("%s&page=%d", currentPageURL, currentPage + 1));
        model.addObject("nextPageDisabled", browseInfo.hasNextPage() ? "" : "disabled");
        model.addObject("links", createPaginationLinksList(currentPage, totalPages, currentPageURL));
        model.setViewName("dateissued-browse");
        return model;

    }
}
