package org.ssu.service;

import org.dspace.browse.BrowseInfo;
import org.dspace.discovery.DiscoverResult;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Service
public class PaginationProcessor {
    private List<String> createPaginationLinksList(Integer currentPage, Integer resultsPerPage, Integer totalPages, String currentPageURL, String offsetKeyword) {
        List<String> links = new ArrayList<>();
        String linkFormat = "<li><a href=\"%s&%s=%d\" class = \"%s\">%d</a></li>";
        String separator = "<li><span>...</span></li>";

        Predicate<Integer> needToDisplayLinkToFirstPage = (current) -> current - 2 > 1;
        Predicate<Integer> needToDisplayGapAfterFirstPage = (current) -> current - 2 > 2;
        BiPredicate<Integer, Integer> needToDisplayGapBeforeLastPage = (current, total) -> current + 2 < total - 1;
        BiPredicate<Integer, Integer> needToDisplayLinkToLastPage = (current, total) -> current + 2 < total;
        BiFunction<Integer, String, String> createLink = (pageNumber, style) -> String.format(linkFormat, currentPageURL, offsetKeyword, (pageNumber - 1) * resultsPerPage, style, pageNumber);

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

    public ModelAndView fillModelWithPaginationData(ModelAndView model, HttpServletRequest request, DiscoverResult discoverResult) {
        long pageTotal = 1 + ((discoverResult.getTotalSearchResults() - 1) / discoverResult.getMaxResults());
        long pageCurrent = 1 + (discoverResult.getStart() / discoverResult.getMaxResults());
        long pageLast = ((pageCurrent + 3) > pageTotal) ? pageTotal : (pageCurrent + 3);
        long pageFirst = ((pageCurrent - 3) > 1) ? (pageCurrent - 3) : 1;

        return fillModelWithPaginationData(model, request, pageFirst != pageCurrent, pageTotal > pageLast, discoverResult.getStart(), discoverResult.getTotalSearchResults(), "start");
    }

    public ModelAndView fillModelWithPaginationData(ModelAndView model, HttpServletRequest request, BrowseInfo browseInfo) {
        return fillModelWithPaginationData(model, request, browseInfo.hasPrevPage(), browseInfo.hasNextPage(), browseInfo.getOffset(), (long) browseInfo.getTotal(), "offset");
    }

    public ModelAndView fillModelWithPaginationData(ModelAndView model, HttpServletRequest request, Boolean hasPreviousPage, Boolean hasNextPage, Integer offset, Long total, String offsetKeyword) {
        Integer perPage = getResultsPerPage(request.getParameter("rpp"));
        String currentPageURL = (request.getRequestURL().toString() + "?" + request.getQueryString())
                .replaceAll("[?&]offset=\\d+", "")
                .replaceAll("[?&]start=\\d+", "")
                .replaceAll("[?&]year=\\d+", "");
        int currentPage = offset / perPage + 1;
        int totalPages = (int) Math.ceil(Double.valueOf(total) / perPage);

        model.addObject("prevPageUrl", String.format("%s&%s=%d", currentPageURL, offsetKeyword, (currentPage - 2) * perPage));
        model.addObject("prevPageDisabled", hasPreviousPage ? "" : "disabled");
        model.addObject("nextPageUrl", String.format("%s&%s=%d", currentPageURL, offsetKeyword, currentPage * perPage));
        model.addObject("nextPageDisabled", hasNextPage ? "" : "disabled");
        model.addObject("links", createPaginationLinksList(currentPage, perPage, totalPages, currentPageURL, offsetKeyword));

        return model;
    }
}
