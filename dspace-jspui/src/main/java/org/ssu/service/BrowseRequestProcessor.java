package org.ssu.service;

import org.dspace.browse.BrowseInfo;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;

import javax.annotation.Resource;
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
    @Resource
    private PaginationProcessor paginationProcessor;

    private Integer getResultsPerPage(String resultsPerPageValueFromRequest) {
        return Optional.ofNullable(resultsPerPageValueFromRequest)
                .map(Integer::valueOf)
                .orElse(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.collectionhome.perpage", 20));
    }

    public ModelAndView fillModelWithData(ModelAndView model, List<ItemResponse> items, BrowseInfo browseInfo, HttpServletRequest request, Boolean isExtendedTable) throws SortException {
        Integer perPage = getResultsPerPage(request.getParameter("rpp"));

        model.addObject("requestUri", request.getRequestURI());
        model.addObject("items", items);
        model.addObject("type", request.getParameter("type"));
        model.addObject("startIndex", browseInfo.getStart());
        model.addObject("finishIndex", browseInfo.getFinish());
        model.addObject("totalItems", browseInfo.getTotal());
        model.addObject("sortedBy", browseInfo.getSortOption());
        model.addObject("sortOrder", request.getParameter("order"));
        model.addObject("rpp", perPage);
        model.addObject("selectedYear", Optional.ofNullable(request.getParameter("starts_with")).map(String::valueOf).orElse(""));
        model.addObject("sortOptions", SortOption.getSortOptions().stream().filter(SortOption::isVisible).collect(Collectors.toSet()));
        model.addObject("queryString", request.getQueryString());
        model.addObject("isExtended", isExtendedTable);

        model = paginationProcessor.fillModelWithPaginationData(model, request, browseInfo);
        model.setViewName("browse");
        return model;
    }
}
