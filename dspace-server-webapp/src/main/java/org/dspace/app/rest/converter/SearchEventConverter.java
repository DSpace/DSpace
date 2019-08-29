/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.PageRest;
import org.dspace.app.rest.model.SearchEventRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.usage.RestUsageSearchEvent;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchEventConverter {

    @Autowired
    private ScopeResolver scopeResolver;

    public RestUsageSearchEvent convert(Context context, HttpServletRequest request, SearchEventRest searchEventRest) {
        RestUsageSearchEvent restUsageSearchEvent = new RestUsageSearchEvent(UsageEvent.Action.SEARCH, request, context,
                                                                             null);
        restUsageSearchEvent.setQuery(searchEventRest.getQuery());
        restUsageSearchEvent.setDsoType(searchEventRest.getDsoType());
        IndexableObject scopeObject = scopeResolver.resolveScope(context, String.valueOf(searchEventRest.getScope()));
        if (scopeObject instanceof DSpaceObject) {
            restUsageSearchEvent.setScope((DSpaceObject) scopeObject);
        }
        restUsageSearchEvent.setConfiguration(searchEventRest.getConfiguration());
        restUsageSearchEvent.setAppliedFilters(convertAppliedFilters(searchEventRest.getAppliedFilters()));
        restUsageSearchEvent.setSort(convertSort(searchEventRest.getSort()));
        restUsageSearchEvent.setPage(convertPage(searchEventRest.getPage()));

        return restUsageSearchEvent;

    }

    private RestUsageSearchEvent.Page convertPage(PageRest page) {
        return new RestUsageSearchEvent.Page(page.getSize(), page.getTotalElements(), page.getTotalPages(),
                                             page.getNumber());
    }

    private RestUsageSearchEvent.Sort convertSort(SearchResultsRest.Sorting sort) {
        return new RestUsageSearchEvent.Sort(sort.getBy(), sort.getOrder());
    }

    private List<RestUsageSearchEvent.AppliedFilter> convertAppliedFilters(
        List<SearchResultsRest.AppliedFilter> appliedFilters) {
        List<RestUsageSearchEvent.AppliedFilter> listToReturn = new LinkedList<>();
        for (SearchResultsRest.AppliedFilter appliedFilter : appliedFilters) {
            RestUsageSearchEvent.AppliedFilter convertedAppliedFilter = new RestUsageSearchEvent.AppliedFilter(
                appliedFilter.getFilter(), appliedFilter.getOperator(), appliedFilter.getValue(),
                appliedFilter.getLabel());
            listToReturn.add(convertedAppliedFilter);
        }
        return listToReturn;
    }
}
