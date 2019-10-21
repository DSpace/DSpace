package org.ssu.service;

import org.dspace.browse.BrowseInfo;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class BrowseRequestProcessor {
    private List<String> createPaginationLinksList(Integer currentPage, Integer resultsPerPage, Integer totalPages, String currentPageURL) {
        List<String> links = new ArrayList<>();
        String linkFormat = "<li><a href=\"%s&offset=%d\" class = \"%s\">%d</a></li>";
        String separator = "<li><span>...</span></li>";

        Predicate<Integer> needToDisplayLinkToFirstPage = (current) -> current - 2 > 1;
        Predicate<Integer> needToDisplayGapAfterFirstPage = (current) -> current - 2 > 2;
        BiPredicate<Integer, Integer> needToDisplayGapBeforeLastPage = (current, total) -> current + 2 < total - 1;
        BiPredicate<Integer, Integer> needToDisplayLinkToLastPage = (current, total) -> current + 2 < total;
        BiFunction<Integer, String, String> createLink = (pageNumber, style) -> String.format(linkFormat, currentPageURL, (pageNumber - 1) * resultsPerPage, style, pageNumber);

        if (needToDisplayLinkToFirstPage.test(currentPage)) links.add(createLink.apply(1, ""));
        if (needToDisplayGapAfterFirstPage.test(currentPage)) links.add(separator);

        for (int cur = Math.max(1, currentPage - 2); cur <= Math.min(totalPages, currentPage + 2); cur++) {
            links.add(createLink.apply(cur, (cur == currentPage) ? "current" : ""));
        }

        if (needToDisplayGapBeforeLastPage.test(currentPage, totalPages)) links.add(separator);
        if (needToDisplayLinkToLastPage.test(currentPage, totalPages)) links.add(createLink.apply(totalPages, ""));
        return links;
    }

    private Integer getResultsPerPage(String resultsPerPageValueFromRequest) {
        return Optional.ofNullable(resultsPerPageValueFromRequest)
                .map(Integer::valueOf)
                .orElse(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.collectionhome.perpage", 20));
    }

    public ModelAndView fillModelWithData(ModelAndView model, List<ItemResponse> items, BrowseInfo browseInfo, HttpServletRequest request, Boolean isExtendedTable) throws SortException {
        Integer perPage = getResultsPerPage(request.getParameter("rpp"));


        String currentPageURL = (request.getRequestURL().toString() + "?" + request.getQueryString())
                .replaceAll("[?&]offset=\\d+", "")
//                .replaceAll("[?&]starts_with=[^&]*", "")
                .replaceAll("[?&]year=\\d+", "");
        int currentPage = browseInfo.getOffset() / perPage + 1;
        int totalPages = (int) Math.ceil(Double.valueOf(browseInfo.getTotal()) / perPage);

        model.addObject("items", items);
        model.addObject("type", request.getParameter("type"));
        model.addObject("startIndex", browseInfo.getStart());
        model.addObject("finishIndex", browseInfo.getFinish());
        model.addObject("totalItems", browseInfo.getTotal());
        model.addObject("sortedBy", browseInfo.getSortOption());
        model.addObject("sortOrder", request.getParameter("sortOrder"));
        model.addObject("rpp", perPage);
        model.addObject("selectedYear", Optional.ofNullable(request.getParameter("starts_with")).map(String::valueOf).orElse(""));
        model.addObject("sortOptions", SortOption.getSortOptions().stream().filter(SortOption::isVisible).collect(Collectors.toSet()));
        model.addObject("prevPageUrl", String.format("%s&offset=%d", currentPageURL, (currentPage - 2) * perPage));
        model.addObject("prevPageDisabled", browseInfo.hasPrevPage() ? "" : "disabled");
        model.addObject("nextPageUrl", String.format("%s&offset=%d", currentPageURL, currentPage * perPage));
        model.addObject("nextPageDisabled", browseInfo.hasNextPage() ? "" : "disabled");
        model.addObject("links", createPaginationLinksList(currentPage, perPage, totalPages, currentPageURL));
        model.addObject("isExtended", isExtendedTable);
        model.setViewName("browse");
        return model;
    }
}
